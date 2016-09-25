(ns structurize.components.home-page
  (:require [structurize.system.utils :refer [process-side-effect side-effect!]]
            [structurize.system.state :refer [track read write!]]
            [structurize.system.browser :refer [change-location!]]
            [structurize.system.comms :refer [send! post!]]
            [structurize.components.utils :as u]
            [structurize.components.image :refer [image]]
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

;; TODO - spec everywhere

;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn home-page [{:keys [config-opts] :as Φ}]
  (r/create-class
   {:component-did-mount #(side-effect! Φ :home-page/did-mount)
    :reagent-render (fn []
                      (log-debug Φ "render home-page")
                      (let [me (track Φ l/view-single
                                      (in [:auth :me]))]

                        [:div.l-cell.l-cell--fill-parent {:style {:position :relative}}
                         [:svg#voronoi {:style {:position :absolute
                                                :height "100%"
                                                :width "100%"
                                                :z-index -1}}]
                         [:div.l-col.l-col--align-center.l-col--padding-top-25
                          [:div.l-row
                           [:span.c-icon.c-icon--diamond.c-icon--h-size-medium.c-icon--margin-right-medium]
                           [:span.c-text.c-text--h-size-medium "Structurize"]]

                          [:div.l-col.l-col--align-center.l-col--margin-top-xx-large
                           (let [path (b/path-for (:routes config-opts) :component-guide)]
                             [:a.c-link.c-link--margin-top-large {:href path
                                                                  :on-click (u/without-default
                                                                             #(side-effect! Φ :home-page/change-location
                                                                                            {:path path}))}
                              [:span.c-icon.c-icon--layers.c-icon--margin-right-x-small]
                              [:span.c-text "Component Guide"]])

                           (let [path (b/path-for (:routes config-opts) :store-concept)]
                             [:a.c-link.c-link--margin-top-large {:href path
                                                                  :on-click (u/without-default
                                                                             #(side-effect! Φ :home-page/change-location
                                                                                            {:path path}))}
                              [:span.c-icon.c-icon--crop.c-icon--margin-right-x-small]
                              [:span.c-text "Design Concepts"]])

                           [:a.c-link.c-link--margin-top-large {:on-click (u/without-propagation
                                                                           #(side-effect! Φ (if me
                                                                                              :home-page/sign-out
                                                                                              :home-page/initialise-sign-in-with-github)))}
                            [:span.c-icon.c-icon--github.c-icon--margin-right-x-small]
                            [:span.c-text (if me "Sign out" "Sign in")]]]]]))}))


;; side-effects ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod process-side-effect :home-page/did-mount [Φ id props]
  (let [{:keys [height width]} (read Φ l/view-single
                                     (in [:viewport]))

        data (clj->js (mapv (fn [] {:x (rand-int width) :y (rand-int height)}) (range 250)))

        svg (d3.select "#voronoi")
        width (.attr svg "width")
        height (.attr svg "height")

        voronoi (-> (d3.geom.voronoi)
                    (.x (fn [d] (aget d "x")))
                    (.y (fn [d] (aget d "y")))
                    #_(.clipExtent (clj->js [[0 0] [700 700]])))

        pathsss (-> svg
                  (.selectAll "paths")
                  (.data (voronoi data))
                  (.enter)
                  (.append "path")
                  (.attr "d" (fn [d i] (str "M" (.join d "L") "Z")))
                  (.datum (fn [d i] (.-point d)))
                  (.attr "fill" "transparent")
                  (.attr "stroke" "#DFDFDF"))]))

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
  [{:keys [config-opts] :as Φ} id props]
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


(defmethod process-side-effect :home-page/change-location
  [{:keys [config-opts] :as Φ} id {:keys [path] :as props}]
  (change-location! Φ {:path path}))
