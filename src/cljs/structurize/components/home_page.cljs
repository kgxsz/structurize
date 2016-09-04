(ns structurize.components.home-page
  (:require [structurize.system.side-effector :refer [process-side-effect side-effect!]]
            [structurize.system.state :refer [track read write!]]
            [structurize.system.browser :refer [change-location!]]
            [structurize.system.comms :refer [send! post!]]
            [structurize.system.utils :as su]
            [structurize.components.utils :as u]
            [structurize.components.image :refer [image]]
            [structurize.components.with-page-load :refer [with-page-load]]
            [structurize.components.grid :refer [grid]]
            [structurize.lens :refer [in]]
            [structurize.types :as t]
            [medley.core :as m]
            [cljs.spec :as s]
            [bidi.bidi :as b]
            [traversy.lens :as l]
            [reagent.core :as r])
  (:require-macros [structurize.components.macros :refer [log-info log-debug log-error]]))


;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn sign-in-with-github [φ]
  (log-debug φ "render sign-in-with-github")
  [:button.c-button {:on-click (u/without-propagation
                                #(side-effect! φ :home-page/initialise-sign-in-with-github))}
   [:div.l-row.l-row--justify-center
    [:div.l-cell.l-cell--margin-right-small.c-icon.c-icon--github]
    "sign in with GitHub"]])


(defn sign-out [Φ]
  (log-debug Φ "render sign-out")
  [:button.c-button {:on-click (u/without-propagation
                                #(side-effect! Φ :home-page/sign-out))}
   [:div.l-row.l-row--justify-center
    [:div.l-cell.l-cell--margin-right-small.c-icon.c-icon--exit]
    "sign out"]])


(defn grid-column [Φ {:keys [width gutter cs]}]
  (log-debug Φ "render pod column")
  [:div {:style {:width width}}
   (doall
    (for [[i c] (m/indexed cs)]
      [:div {:key i
             :style {:margin-top gutter}}
       [c Φ]]))]

  #_[:button.c-button {:on-click (u/without-propagation
                                #(side-effect! Φ :home-page/sign-out))}
   [:div.l-row.l-row--justify-center
    [:div.l-cell.l-cell--margin-right-small.c-icon.c-icon--exit]
    "sign out"]])


(defn home-page [Φ]
  [with-page-load Φ
   (fn [Φ]
     (let [me (track Φ l/view-single
                       (in [:auth :me]))
           star (track Φ l/view-single
                         (in [:playground :star]))
           heart (track Φ l/view-single
                          (in [:playground :heart]))
           pong (track Φ l/view-single
                         (in [:playground :pong]))]

       (log-debug Φ "render home-page")

       [:div
        [grid Φ
         {:center {:hidden #{}
                   :c (fn [Φ {:keys [width col-n col-width gutter margin-left margin-right]}]
                        [:div.l-row.l-row--justify-space-between {:style {:width width
                                                                          :padding-left gutter
                                                                          :padding-right gutter
                                                                          :margin-left margin-left
                                                                          :margin-right margin-right}}
                         (doall
                          (for [i (range col-n)]
                            ^{:key i}
                            [grid-column Φ
                             {:width col-width
                              :gutter gutter
                              :cs [(fn [Φ]
                                     [:div {:style {:height (+ 200 (rand 200))
                                                    :background-color :pink}}])
                                   (fn [Φ]
                                     [:div {:style {:height (+ 200 (rand 200))
                                                    :background-color :purple}}])
                                   (fn [Φ]
                                     [:div {:style {:height (+ 200 (rand 200))
                                                    :background-color :green}}])]}]))])}
          :left {:hidden #{:xs :sm}
                 :c (fn [Φ {:keys [width col-n col-width gutter margin-left]}]
                      [:div.l-col.l-col--align-start {:style {:width width
                                                              :padding-left gutter
                                                              :margin-left margin-left}}
                       [grid-column Φ
                        {:width col-width
                         :gutter gutter
                         :cs [(fn [Φ]
                                [:div {:style {:height 240
                                               :background-color :green}}])
                              (fn [Φ]
                                [:div {:style {:height 340
                                               :background-color :red}}])]}]])}
          :right {:hidden #{:xs :sm :md}
                  :c (fn [Φ {:keys [width col-n col-width gutter margin-right]}]
                       [:div.l-col.l-col--align-end {:style {:width width
                                                             :padding-right gutter
                                                             :margin-right margin-right}}
                        [grid-column Φ
                         {:width col-width
                          :gutter gutter
                          :cs [(fn [Φ]
                                 [:div {:style {:height 240
                                                :background-color :orange}}])
                               (fn [Φ]
                                 [:div {:style {:height 340
                                                :background-color :blue}}])]}]])}
          :top {:hidden #{}
                :c (fn [Φ {:keys [width col-n col-width gutter margin-left margin-right]}]
                     [:div {:style {:height 200
                                    :width width
                                    :margin-left margin-left
                                    :margin-right margin-right
                                    :background-color :purple}}
                      ":top"])}
          :bottom {:hidden #{:xs}
                   :c (fn [Φ {:keys [width col-n col-width gutter margin-left margin-right]}]
                        [:div {:style {:height 200
                                       :width width
                                       :margin-left margin-left
                                       :margin-right margin-right
                                       :background-color :aqua}}
                         ":bottom"])}}]

        #_[:div.l-col.l-col--justify-center
         (if me
           [:div.l-col.l-col--align-center.c-hero
            [:div.c-hero__avatar
             [image Φ {:+image (in [:home-page :hero-avatar-image])
                         :src (:avatar-url me)}]]
            [:div.c-hero__caption "Hello @" (:login me)]]

           [:div.c-hero
            [:div.c-icon.c-icon--mustache.c-icon--h-size-xx-large]
            [:div.c-hero__caption "Hello there"]])

         [:div.l-col.l-col--align-center
          (if me
            [sign-out Φ]
            [sign-in-with-github Φ])

          [:div.l-cell.l-cell--margin-top-medium
           [:button.c-button {:on-click (u/without-propagation
                                         #(side-effect! Φ :home-page/inc-item
                                                        {:path [:playground :star]
                                                         :item-name "star"}))}
            [:div.l-row.l-row--justify-center
             [:div.l-cell.l-cell--margin-right-small.c-icon.c-icon--star]
             star]]]

          [:div.l-cell.l-cell--margin-top-medium
           [:button.c-button {:on-click (u/without-propagation
                                         #(side-effect! Φ :home-page/inc-item
                                                        {:path [:playground :heart]
                                                         :item-name "heart"}))}
            [:div.l-row.l-row--justify-center
             [:div.l-cell.l-cell--margin-right-small.c-icon.c-icon--heart]
             heart]]]

          [:div.l-cell.l-cell--margin-top-medium
           [:button.c-button {:on-click (u/without-propagation
                                         #(side-effect! Φ :home-page/ping {}))}
            [:div.l-row.l-row--justify-center
             [:div.l-cell.l-cell--margin-right-small.c-icon.c-icon--heart-pulse]
             pong]]]]]]
       ))])


;; side-effects ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod process-side-effect :home-page/initialise-sign-in-with-github
  [{:keys [config-opts] :as Φ} id props]
  (send! Φ :auth/initialise-sign-in-with-github
         {}
         {:on-success (fn [[_ {:keys [client-id attempt-id scope redirect-prefix]}]]
                        (let [redirect-uri (str redirect-prefix (b/path-for (:routes config-opts) :sign-in-with-github))]
                          (change-location! Φ {:prefix "https://github.com"
                                               :path "/login/oauth/authorize"
                                               :query {:client_id client-id
                                                       :state attempt-id
                                                       :scope scope
                                                       :redirect_uri redirect-uri}})))
          :on-failure (fn [reply]
                        (write! Φ :auth/sign-in-with-github-failed
                                (fn [x]
                                  (assoc-in x [:auth :sign-in-with-github-failed?] true))))}))


(defmethod process-side-effect :home-page/sign-out
  [Φ id props]
  (post! Φ "/sign-out"
         {}
         {:on-success (fn [response]
                        (write! Φ :auth/sign-out
                                (fn [x]
                                  (assoc x :auth {}))))
          :on-failure (fn [response]
                        (write! Φ :auth/sign-out-failed
                                (fn [x]
                                  (assoc-in x [:auth :sign-out-status] :failed))))}))


(defmethod process-side-effect :home-page/inc-item
  [Φ id {:keys [path item-name] :as props}]
  (let [mutation-id (keyword (str "playground/inc-" item-name))]
    (write! Φ mutation-id
            (fn [x]
              (update-in x path inc)))))


(defmethod process-side-effect :home-page/ping
  [Φ id props]
  (let [ping (read Φ l/view-single
                   (in [:playground :ping]))]

    (write! Φ :playground/ping
            (fn [x]
              (update-in x [:playground :ping] inc)))

    (send! Φ :playground/ping
           {:ping (inc ping)}
           {:on-success (fn [[id payload]]
                          (write! Φ :playground/pong
                                  (fn [x]
                                    (assoc-in x [:playground :pong] (:pong payload)))))
            :on-failure (fn [reply] (write! Φ :playground/ping-failed
                                           (fn [x]
                                             (assoc-in x [:playground :ping-status] :failed))))})))
