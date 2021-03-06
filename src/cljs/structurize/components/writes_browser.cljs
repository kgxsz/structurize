(ns structurize.components.writes-browser
  (:require [structurize.system.utils :refer [process-side-effect side-effect!]]
            [structurize.system.state :refer [track read write!]]
            [structurize.system.browser :refer [change-location!]]
            [structurize.system.comms :refer [send! post!]]
            [structurize.components.utils :as u]
            [structurize.lens :refer [in]]
            [structurize.types :as t]
            [cljs.spec :as s]
            [traversy.lens :as l]
            [reagent.core :as r])
  (:require-macros [structurize.components.macros :refer [log-info log-debug log-error]]))

;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn writes-browser [φ]
  (log-debug φ "mount writes-browser")
  (fn []
    (let [writes (track φ l/view
                        (l/*> (in [:writes]) l/all-values))
          read-write-index (track φ l/view-single
                                  (in [:read-write-index]))

          track-index (track φ l/view-single
                             (in [:track-index]))
          time-travelling? (track φ l/view-single
                                  (in [:time-travelling?]))
          end-of-time? (= read-write-index track-index)
          beginning-of-time? (zero? track-index)]

      (log-debug φ "render writes-browser")

      [:div.l-row.c-writes-browser
       [:div.l-col.l-col--padding-medium.c-writes-browser__controls
        [:div.c-writes-browser__controls__item.c-writes-browser__controls__item--green
         {:class (u/->class {:c-writes-browser__controls__item--opaque (not time-travelling?)
                             :c-writes-browser__controls__item--clickable (and end-of-time? time-travelling?)})
          :on-click (when (and time-travelling? end-of-time?)
                      (u/without-propagation
                       #(side-effect! φ :writes-browser/stop-time-travelling)))}
         [:div.c-icon.c-icon--control-play.c-icon--color-olivedrab]]

        [:div.c-writes-browser__controls__item.c-writes-browser__controls__item--yellow
         {:class (when time-travelling? (u/->class {:c-writes-browser__controls__item--opaque time-travelling?
                                                    :c-writes-browser__controls__item--clickable (not end-of-time?)}))
          :on-click (when-not end-of-time?
                      (u/without-propagation
                       #(side-effect! φ :writes-browser/go-forward-in-time)))}
         [:div.c-icon.c-icon--control-next.c-icon--color-goldenrod]]

        [:div.c-writes-browser__controls__item.c-writes-browser__controls__item--yellow
         {:class (u/->class {:c-writes-browser__controls__item--opaque time-travelling?
                             :c-writes-browser__controls__item--clickable (not beginning-of-time?)})
          :on-click (when-not beginning-of-time?
                      (u/without-propagation
                       #(side-effect! φ :writes-browser/go-back-in-time)))}
         [:div.c-icon.c-icon--control-prev.c-icon--color-goldenrod]]]

       [:div.l-row
        (doall
         (for [{:keys [id n]} (take-last track-index (sort-by :n > writes))]
           [:div.c-writes-browser__item {:key n}
            [:div.c-writes-browser__item__superscript
             [:span.c-text.c-text--margin-right-xx-small.c-text--color-white "Δ"]
             [:span.c-text.c-text--color-white n]]

            [:div.c-writes-browser__item__pill
             [:div.c-writes-browser__item__pill__content
              (pr-str id)]]]))]])))


;; side-effects ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod process-side-effect :writes-browser/go-back-in-time
  [Φ id props]
  (let [track-index (max 0 (dec (read Φ l/view-single
                                      (in [:track-index]))))
        {:keys [paths upstream-paths]} (read Φ l/view-single
                                             (in [:writes track-index]))]
    (write! Φ :writes-browser/go-back-in-time
            (fn [x]
              (-> x
                  (assoc-in [:time-travelling?] true)
                  (assoc-in [:track-index] track-index)
                  (assoc-in [:app-browser-props :written] {:paths (or paths #{})
                                                           :upstream-paths (or upstream-paths #{})}))))))


(defmethod process-side-effect :writes-browser/go-forward-in-time
  [Φ id props]
  (let [read-write-index (read Φ l/view-single
                               (in [:read-write-index]))
        track-index (min read-write-index (inc (read Φ l/view-single
                                                     (in [:track-index]))))
        {:keys [paths upstream-paths]} (read Φ l/view-single
                                             (in [:writes track-index]))]
    (write! Φ :writes-browser/go-forward-in-time
            (fn [x]
              (-> x
                  (assoc-in [:track-index] track-index)
                  (assoc-in [:app-browser-props :written] {:paths (or paths #{})
                                                           :upstream-paths (or upstream-paths #{})}))))))


(defmethod process-side-effect :writes-browser/stop-time-travelling
  [Φ id props]
  (let [read-write-index (read Φ l/view-single
                               (in [:read-write-index]))
        {:keys [paths upstream-paths]} (read Φ l/view-single
                                             (in [:writes read-write-index]))]
    ;; What do you do if you're not at the end of time here?
    (write! Φ :writes-browser/stop-time-travelling
            (fn [x]
              (-> x
                  (assoc-in [:time-travelling?] false)
                  (assoc-in [:track-index] read-write-index)
                  (assoc-in [:app-browser-props :written] {:paths (or paths #{})
                                                           :upstream-paths (or upstream-paths #{})}))))))
