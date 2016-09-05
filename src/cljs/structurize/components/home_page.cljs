(ns structurize.components.home-page
  (:require [structurize.system.side-effector :refer [process-side-effect side-effect!]]
            [structurize.system.state :refer [track read write!]]
            [structurize.system.browser :refer [change-location!]]
            [structurize.system.comms :refer [send! post!]]
            [structurize.system.utils :as su]
            [structurize.components.utils :as u]
            [structurize.components.image :refer [image]]
            [structurize.components.with-page-load :refer [with-page-load]]
            [structurize.components.triptych :refer [triptych triptych-column]]
            [structurize.components.masthead :refer [masthead]]
            [structurize.lens :refer [in]]
            [structurize.types :as t]
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


(defn pod [Φ {:keys [orientation colour size width] :as props}]
  (log-debug Φ "mount pod")
  (r/create-class
   {:component-did-mount #(side-effect! Φ :home-page/pod-did-mount (merge props {:node (r/dom-node %)}))
    :reagent-render (fn []
                      (log-debug Φ "render pod")
                      [:div {:style {:height (+ 200 (rand 200))}}])}))


(defn aux-pod [Φ]
  [pod Φ {:orientation "2/8" :colour "#495159" :width 1 :size 5}])


(defn content-pod [Φ]
  (let [colour (rand-nth ["#B39EB5" "#F49AC2" "#FF6961" "#03C03C" "#AEC6CF"
                          "#836953" "#FDFD96" "#C23B22" "#DEA5A4" "#77DD77"
                          "#FFB347" "#B19CD9" "#779ECB" "#966FD6" "#CFCFC4"])]
    [pod Φ {:orientation "6/8" :colour colour :width 1 :size 5}]))


(defn home-page [Φ]
  [with-page-load Φ
   (fn [Φ]
     (let [me (track Φ l/view-single
                     (in [:auth :me]))]

       (log-debug Φ "render home-page")

       [:div.l-cell.l-cell--margin-bottom-medium.c-page

        [:div.c-header
         [triptych Φ {:left {:hidden #{:xs :sm :md}
                             :c (fn [Φ {:keys [width col-n col-width gutter margin-left]}]
                                  [:div {:style {:width width
                                                 :margin-left margin-left
                                                 :padding-left gutter
                                                 :padding-top 6
                                                 :padding-bottom 6}}
                                   [:div.l-cell.l-cell--fill-parent {:style {:background-color "#F9F9F9"}}]])}
                      :center {:c (fn [Φ {:keys [width col-n col-width gutter margin-left margin-right]}]
                                    [:div {:style {:width width
                                                   :margin-left margin-left
                                                   :margin-right margin-right
                                                   :padding-left gutter
                                                   :padding-right gutter
                                                   :padding-top 6
                                                   :padding-bottom 6}}
                                     [:div.l-cell.l-cell--fill-parent {:style {:background-color "#EEE"}}]])}
                      :right {:hidden #{:xs :sm :md}
                              :c (fn [Φ {:keys [width col-n col-width gutter margin-right]}]
                                   [:div {:style {:width width
                                                  :margin-right margin-right
                                                  :padding-right gutter
                                                  :padding-top 6
                                                  :padding-bottom 6}}
                                    [:div.l-cell.l-cell--fill-parent {:style {:background-color "#F9F9F9"}}]])}}]]

        [:div.c-hero
         [triptych Φ {:center {:hidden #{}
                               :c (fn [Φ {:keys [width col-n col-width gutter margin-left margin-right]}]
                                    (let [src (rand-nth ["images/hero-1.png" "images/hero-2.png" "images/hero-3.png"
                                                         "images/hero-4.png" "images/hero-5.png" "images/hero-6.png"
                                                         "images/hero-7.png" "images/hero-8.png" "images/hero-9.png"])]
                                      [:div {:style {:width (+ width margin-left margin-right)}}
                                       [image Φ {:+image (in [:home-page :hero-image])
                                                 :src src}]]))}}]]

        [masthead Φ]

        [:div.c-page-content
         [triptych Φ {:left {:hidden #{:xs :sm}
                             :c (fn [Φ {:keys [width col-n col-width gutter margin-left]}]
                                  [:div.l-col.l-col--align-start {:style {:width width
                                                                          :padding-left gutter
                                                                          :margin-left margin-left}}
                                   [triptych-column Φ
                                    {:width col-width
                                     :gutter gutter
                                     :cs [aux-pod aux-pod]}]])}
                      :center {:hidden #{}
                               :c (fn [Φ {:keys [width col-n col-width gutter margin-left margin-right]}]
                                    [:div.l-row.l-row--justify-space-between {:style {:width width
                                                                                      :padding-left gutter
                                                                                      :padding-right gutter
                                                                                      :margin-left margin-left
                                                                                      :margin-right margin-right}}
                                     (doall
                                      (for [i (range col-n)]
                                        ^{:key i}
                                        [triptych-column Φ
                                         {:width col-width
                                          :gutter gutter
                                          :cs (repeat 6 content-pod)}]))])}
                      :right {:hidden #{:xs :sm :md}
                              :c (fn [Φ {:keys [width col-n col-width gutter margin-right]}]
                                   [:div.l-col.l-col--align-end {:style {:width width
                                                                         :padding-right gutter
                                                                         :margin-right margin-right}}
                                    [triptych-column Φ
                                     {:width col-width
                                      :gutter gutter
                                      :cs [aux-pod]}]])}}]]]))])


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

(defmethod process-side-effect :home-page/pod-did-mount [Φ id {:keys [node size width orientation colour] :as props}]
  (let [sel (d3.select node)
        t (doto (textures.lines)
            (.size size)
            (.strokeWidth width)
            (.orientation orientation)
            (.stroke colour))
        svg (doto (.append sel "svg")
              (.style "height" "100%")
              (.style "width" "100%")
              (.call t))
        r (doto (.append svg "rect")
            (.attr "x" 0)
            (.attr "y" 0)
            (.attr "width" "100%")
            (.attr "height" "100%")
            (.style "fill" (.url t)))]))
