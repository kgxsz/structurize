(ns structurize.components.writes-browser
  (:require [structurize.system.side-effector :refer [process-side-effect side-effect!]]
            [structurize.system.state :refer [track read write!]]
            [structurize.system.browser :refer [change-location!]]
            [structurize.system.comms :refer [send! post!]]
            [structurize.system.utils :as su]
            [structurize.components.utils :as u]
            [structurize.lens :refer [in]]
            [structurize.types :as t]
            [cljs.spec :as s]
            [traversy.lens :as l]
            [reagent.core :as r])
  (:require-macros [structurize.components.macros :refer [log-info log-debug log-error]]))

;; TODO - don't pass anon fns
;; TODO - co-locate CSS
;; TODO - use BEM utility
;; TODO - spec everywhere

;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn writes-browser [φ]
  (log-debug φ "mount writes-browser")
  (fn []
    (let [writes (track φ l/view
                        (l/*> (in [:tooling :writes]) l/all-values))
          read-write-index (track φ l/view-single
                                  (in [:tooling :read-write-index]))
          track-index (track φ l/view-single
                             (in [:tooling :track-index]))
          time-travelling? (track φ l/view-single
                                  (in [:tooling :time-travelling?]))
          end-of-time? (= read-write-index track-index)
          beginning-of-time? (zero? track-index)]

      (log-debug φ "render writes-browser")

      [:div.l-row.c-writes-browser
       [:div.l-col.c-writes-browser__controls
        [:div.c-writes-browser__controls__item.c-writes-browser__controls__item--green
         {:class (u/->class {:c-writes-browser__controls__item--opaque (not time-travelling?)
                             :c-writes-browser__controls__item--clickable (and end-of-time? time-travelling?)})
          :on-click (when (and time-travelling? end-of-time?)
                      (u/without-propagation
                       #(side-effect! φ :writes-browser/stop-time-travelling)))}
         [:div.c-icon.c-icon--control-play.c-icon--p-size-small.c-icon--color-dark-green]]

        [:div.c-writes-browser__controls__item.c-writes-browser__controls__item--yellow
         {:class (when time-travelling? (u/->class {:c-writes-browser__controls__item--opaque time-travelling?
                                                    :c-writes-browser__controls__item--clickable (not end-of-time?)}))
          :on-click (when-not end-of-time?
                      (u/without-propagation
                       #(side-effect! φ :writes-browser/go-forward-in-time)))}
         [:div.c-icon.c-icon--control-next.c-icon--p-size-small.c-icon--color-dark-yellow]]

        [:div.c-writes-browser__controls__item.c-writes-browser__controls__item--yellow
         {:class (u/->class {:c-writes-browser__controls__item--opaque time-travelling?
                             :c-writes-browser__controls__item--clickable (not beginning-of-time?)})
          :on-click (when-not beginning-of-time?
                      (u/without-propagation
                       #(side-effect! φ :writes-browser/go-back-in-time)))}
         [:div.c-icon.c-icon--control-prev.c-icon--p-size-small.c-icon--color-dark-yellow]]]

       [:div.l-row
        (doall
         (for [{:keys [id n]} (take-last track-index (sort-by :n > writes))]
           [:div.l-col.c-writes-browser__item {:key n}
            [:div.c-writes-browser__pill-superscript
             [:span.c-writes-browser__pill-superscript__symbol "Δ"]
             [:span n]]

            [:div.c-writes-browser__pill
             [:div.c-writes-browser__pill__content
              (pr-str id)]]]))]])))


;; side-effects ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod process-side-effect :writes-browser/go-back-in-time
  [Φ id props]
  (let [track-index (max 0 (dec (read Φ l/view-single
                                      (in [:tooling :track-index]))))
        {:keys [paths upstream-paths]} (read Φ l/view-single
                                             (in [:tooling :writes track-index]))]
    (write! Φ :writes-browser/go-back-in-time
            (fn [x]
              (-> x
                  (assoc-in [:tooling :time-travelling?] true)
                  (assoc-in [:tooling :track-index] track-index)
                  (assoc-in [:tooling :app-browser-props :written] {:paths (or paths #{})
                                                                    :upstream-paths (or upstream-paths #{})}))))))


(defmethod process-side-effect :writes-browser/go-forward-in-time
  [Φ id props]
  (let [read-write-index (read Φ l/view-single
                               (in [:tooling :read-write-index]))
        track-index (min read-write-index (inc (read Φ l/view-single
                                                     (in [:tooling :track-index]))))
        {:keys [paths upstream-paths]} (read Φ l/view-single
                                             (in [:tooling :writes track-index]))]
    (write! Φ :writes-browser/go-forward-in-time
            (fn [x]
              (-> x
                  (assoc-in [:tooling :track-index] track-index)
                  (assoc-in [:tooling :app-browser-props :written] {:paths (or paths #{})
                                                                    :upstream-paths (or upstream-paths #{})}))))))


(defmethod process-side-effect :writes-browser/stop-time-travelling
  [Φ id props]
  (let [read-write-index (read Φ l/view-single
                               (in [:tooling :read-write-index]))
        {:keys [paths upstream-paths]} (read Φ l/view-single
                                             (in [:tooling :writes read-write-index]))]
    ;; What do you do if you're not at the end of time here?
    (write! Φ :writes-browser/stop-time-travelling
            (fn [x]
              (-> x
                  (assoc-in [:tooling :time-travelling?] false)
                  (assoc-in [:tooling :track-index] read-write-index)
                  (assoc-in [:tooling :app-browser-props :written] {:paths (or paths #{})
                                                                    :upstream-paths (or upstream-paths #{})}))))))
