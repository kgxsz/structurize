(ns structurize.components.root-component
  (:require [cemerick.url :refer [url]]
            [reagent.core :as r]
            [taoensso.timbre :as log]
            [cljs.core.async :as a])
  (:require-macros [cljs.core.async.macros :refer [go]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; other components

#_(defn login-with-github-component [s]
  (log/info "mount login-with-github-component")
  [:button
   [:a
    {:href (-> (url (get-in system [:config-opts :general :github-auth-url]))
               (assoc :query {:client_id (get-in system [:config-opts :general :github-auth-client-id])})
               str)}
    "Login with Github!"]])



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; root component


(defn click-counter-a [{:keys [state <event]}]
  (log/debug "mount/render click-counter-a")
  (let [!click-count (:!click-count-a state)]
    [:button
     {:on-click #(go (a/>! <event [:inc-click-count/c {}]))}
     (str "clicks: " @!click-count)]))


(defn click-counter-b [{:keys [state <event]}]
  (log/debug "mount/render click-counter-b")
  (let [!click-count (:!click-count-b state)]
    [:button
     {:on-click #(go (a/>! <event [:inc-click-count/b {}]))}
     (str "clicks: " @!click-count)]))


(defn root-component [Δ]
  (log/debug "mount/render root-component")
  [:div
   [:h1 "Front end ready!"]
   [click-counter-a Δ]
   [click-counter-b Δ]])
