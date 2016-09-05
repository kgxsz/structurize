(ns structurize.components.home-page
  (:require [structurize.system.side-effector :refer [process-side-effect side-effect!]]
            [structurize.system.state :refer [track read write!]]
            [structurize.system.browser :refer [change-location!]]
            [structurize.system.comms :refer [send! post!]]
            [structurize.system.utils :as su]
            [structurize.components.utils :as u]
            [structurize.components.image :refer [image]]
            [structurize.components.with-page-load :refer [with-page-load]]
            [structurize.components.triptych :refer [triptych]]
            [structurize.components.header :refer [header]]
            [structurize.components.hero :refer [hero]]
            [structurize.components.masthead :refer [masthead]]
            [structurize.components.pod :refer [pod]]
            [structurize.lens :refer [in]]
            [structurize.types :as t]
            [cljs.spec :as s]
            [bidi.bidi :as b]
            [traversy.lens :as l]
            [reagent.core :as r])
  (:require-macros [structurize.components.macros :refer [log-info log-debug log-error]]))


;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#_(defn sign-in-with-github [φ]
  (log-debug φ "render sign-in-with-github")
  [:button.c-button {:on-click (u/without-propagation
                                #(side-effect! φ :home-page/initialise-sign-in-with-github))}
   [:div.l-row.l-row--justify-center
    [:div.l-cell.l-cell--margin-right-small.c-icon.c-icon--github]
    "sign in with GitHub"]])


#_(defn sign-out [Φ]
  (log-debug Φ "render sign-out")
  [:button.c-button {:on-click (u/without-propagation
                                #(side-effect! Φ :home-page/sign-out))}
   [:div.l-row.l-row--justify-center
    [:div.l-cell.l-cell--margin-right-small.c-icon.c-icon--exit]
    "sign out"]])


(defn home-page-left [Φ {:keys [width col-n col-width gutter margin-left]}]
  [:div.l-col.l-col--align-start {:style {:width width
                                          :padding-left gutter
                                          :margin-left margin-left}}
   [:div {:style {:width col-width}}
    (doall
     (for [i (range 2)]
       [:div {:key i
              :style {:margin-top gutter}}
        [pod Φ {:+pod (in [:aux-pod i])}]]))]])


(defn home-page-center [Φ {:keys [width col-n col-width gutter margin-left margin-right]}]
  [:div.l-row.l-row--justify-space-between {:style {:width width
                                                    :padding-left gutter
                                                    :padding-right gutter
                                                    :margin-left margin-left
                                                    :margin-right margin-right}}
   (doall
    (for [col-i (range col-n)]
      [:div {:key col-i
             :style {:width col-width}}
       (doall
        (for [pod-i (range 2)]
          [:div {:key pod-i
                 :style {:margin-top gutter}}
           [pod Φ {:+pod (in [:pods col-i pod-i])}]]))]))])


(defn home-page-right [Φ {:keys [width col-n col-width gutter margin-right]}]
  [:div.l-col.l-col--align-end {:style {:width width
                                        :padding-right gutter
                                        :margin-right margin-right}}
   [:div {:style {:width col-width}}
    (doall
     (for [i (range 1)]
       [:div {:key i
              :style {:margin-top gutter}}
        [pod Φ {:+pod (in [:aux-pod 3])}]]))]])


(defn home-page [Φ]
  [with-page-load Φ
   (fn [Φ]
     (log-debug Φ "render home-page")
     [:div.c-page
      [header Φ]
      [hero Φ]
      [masthead Φ]
      [triptych Φ {:left {:hidden #{:xs :sm}
                          :c home-page-left}
                   :center {:hidden #{}
                            :c home-page-center}
                   :right {:hidden #{:xs :sm :md}
                           :c home-page-right}}]])])


;; side-effects ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#_(defmethod process-side-effect :home-page/initialise-sign-in-with-github
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
