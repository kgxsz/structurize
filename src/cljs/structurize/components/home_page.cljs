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
            [structurize.styles.vars :refer [vars]]
            [garden.color :as c]
            [cljs.spec :as s]
            [bidi.bidi :as b]
            [traversy.lens :as l]
            [reagent.core :as r])
  (:require-macros [structurize.components.macros :refer [log-info log-debug log-error]]))

;; TODO - spec everywhere

;; d3 ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-voronoi [Φ {:keys [node width height]}]
  (let [svg (d3.select node)

        focus-x (/ width 2)
        focus-y (* height 0.35)
        grey-scale-max 240
        grey-scale-min 190
        point-n (int (/ (* width height) 10000))
        radial-max (js/Math.sqrt (+ (js/Math.pow (max focus-x (- width focus-x)) 2)
                                    (js/Math.pow (max focus-y (- height focus-y)) 2)))
        scale-factor (/ (- grey-scale-max grey-scale-min) radial-max)

        grey-scale (fn [[x y]]
                     (let [r (js/Math.sqrt (+ (js/Math.pow (- x focus-x) 2)
                                              (js/Math.pow (- y focus-y) 2)))]
                       (int (- grey-scale-max (* scale-factor r)))))

        points (clj->js
                (repeatedly point-n (fn []
                                      (let [x (rand-int width)
                                            y (rand-int height)
                                            z (grey-scale [x y])
                                            fill (c/rgb->hex {:red z :green z :blue z})]
                                        {:x x :y y :fill fill}))))

        voronoi (-> (d3.geom.voronoi)
                    (.x (fn [d] (aget d "x")))
                    (.y (fn [d] (aget d "y"))))

        draw-paths (fn [paths]
                     (-> paths
                         (.attr "d" (fn [d i] (str "M" (.join d "L") "Z")))
                         (.datum (fn [d i] (aget d "point")))
                         (.attr "fill" (fn [d] (aget d "fill")))))]
    (-> svg
        (.selectAll "paths")
        (.data (voronoi points))
        (.enter)
        (.append "path")
        (.call draw-paths))))


;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn voronoi [{:keys [config-opts] :as Φ} {:keys [width height]}]
  (r/create-class
   {:component-did-mount #(make-voronoi Φ {:node (r/dom-node %) :width width :height height})
    :reagent-render (fn []
                      (log-debug Φ "render voronoi")
                      [:svg.l-cell.l-cell--fill-parent])}))


(defn home-page [{:keys [config-opts] :as Φ}]
  (log-debug Φ "render home-page")
  (let [me (track Φ l/view-single
                  (in [:auth :me]))
        {:keys [width height]} (track Φ l/view-single
                                      (in [:viewport]))]

    [:div.l-cell.l-cell--height-100.c-home-page
     [voronoi Φ {:width width :height height}]
     [:div.l-overlay
      [:div.l-col.l-col--width-100.l-col--align-center.l-col--padding-top-25
       [:div.l-row.l-row--align-center
        [:span.c-icon.c-icon--diamond.c-icon--h-size-medium.c-icon--margin-right-small]
        [:span.c-icon.c-icon--diamond.c-icon--h-size-large]
        [:span.c-icon.c-icon--diamond.c-icon--h-size-medium.c-icon--margin-left-small]]
       [:span.c-text.c-text--h-size-medium.c-text--margin-top-small "Structurize"]

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
         [:span.c-text (if me "Sign out" "Sign in")]]]]]]))


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
