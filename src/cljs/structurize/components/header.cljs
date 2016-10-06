(ns structurize.components.header
  (:require [structurize.system.utils :refer [process-side-effect side-effect!]]
            [structurize.system.state :refer [track read write!]]
            [structurize.system.browser :refer [change-location!]]
            [structurize.system.comms :refer [send! post!]]
            [structurize.components.utils :as u]
            [structurize.components.triptych :refer [triptych]]
            [structurize.lens :refer [in]]
            [structurize.types :as t]
            [bidi.bidi :as b]
            [cljs.spec :as s]
            [traversy.lens :as l]
            [reagent.core :as r])
  (:require-macros [structurize.components.macros :refer [log-info log-debug log-error]]))

(defn header-left [Φ {:keys [width col-n col-width gutter margin-left]}]
  [:div.c-header__item {:style {:width (- width gutter)
                                :margin-left (+ margin-left gutter)}}])


(defn header-center [Φ {:keys [width col-n col-width gutter margin-left margin-right]}]
  [:div.c-header__item  {:style {:width (- width gutter gutter)
                                 :margin-left (+ margin-left gutter)
                                 :margin-right (+ margin-right gutter)}}])


(defn header-right [Φ {:keys [width col-n col-width gutter margin-right]}]
  (let [me (track Φ l/view-single
                  (in [:auth :me]))]
    [:div.c-header__item {:on-click (u/without-propagation
                                     #(side-effect! Φ (if me
                                                        :header/sign-out
                                                        :header/initialise-sign-in-with-github)))
                          :style {:width (- width gutter)
                                  :margin-right (+ margin-right gutter)}}]))


(defn header [Φ]
  (log-debug Φ "render header")
  [:div.l-cell.l-cell--padding-top-x-small.l-cell--padding-bottom-x-small.c-header
   [triptych Φ {:left {:hidden #{:xs :sm :md}
                       :c [header-left]}
                :center {:c [header-center]}
                :right {:hidden #{:xs :sm :md}
                        :c [header-right]}}]])

;; side-effects ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod process-side-effect :header/initialise-sign-in-with-github
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


(defmethod process-side-effect :header/sign-out
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
