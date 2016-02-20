(ns structurize.system.config-opts
  (:require [clojure.edn :as edn]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))


(defn general-config-opts [config]
  {:github-auth-client-id (:github-auth-client-id config)
   :github-auth-client-secret (:github-auth-client-secret config)
   :github-auth-scope "user:email"})


(defn comms-config-opts [config]
  {:chsk-opts {:packer :edn}})


(defn server-config-opts [config]
  {:http-kit-opts {:port (:port config)}
   :middleware-opts {:params {:urlencoded true
                              :nested true
                              :keywordize true}
                     :security {:anti-forgery {:read-token (fn [req] (-> req :params :csrf-token))}
                                :xss-protection {:enable? true, :mode :block}
                                :frame-options :sameorigin
                                :content-type-options :nosniff}
                     :session {:flash true
                               :cookie-attrs {:http-only true
                                              :max-age 3600}}
                     :static {:resources "public"}
                     :responses {:not-modified-responses true
                                 :absolute-redirects true
                                 :content-types true
                                 :default-charset "utf-8"}}})


(defn load-config []
  (let [home (System/getProperty "user.home")
        public-config (-> "resources/config.edn" slurp edn/read-string)
        private-config (-> (str home "/.lein/structurize/config.edn") slurp edn/read-string)]
    (merge public-config private-config)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord ConfigOpts []
  component/Lifecycle

  (start [component]
    (log/info "initialising config-opts")
    (let [config (load-config)]
      (assoc component
             :general (general-config-opts config)
             :comms (comms-config-opts config)
             :server (server-config-opts config))))

  (stop [component] component))
