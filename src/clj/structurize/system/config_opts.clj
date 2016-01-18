(ns structurize.system.config-opts
  (:require [clojure.edn :as edn]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component config opts

(def config
  {:port 3000
   :github-auth-client-id "5983375a79d32f8c66ba"})


(defn general-config-opts [config]
  {:github-access-token-url "https://github.com/login/oauth/access_token"
   :github-auth-client-id (:github-auth-client-id config)
   :github-auth-client-secret (:github-auth-client-secret config)})


(defn chsk-conn-config-opts [config]
  {})


(defn handler-config-opts [config]
  {:middleware-opts {:params {:urlencoded true
                              :nested true
                              :keywordize true}
                     :security {:anti-forgery true
                                :xss-protection {:enable? true, :mode :block}
                                :frame-options :sameorigin
                                :content-type-options :nosniff}
                     :static {:resources "public"}
                     :responses {:not-modified-responses true
                                 :absolute-redirects true
                                 :content-types true
                                 :default-charset "utf-8"}}})


(defn server-config-opts [config]
   {:port (:port config)})



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; config loading


(defn load-config []
  ;; TODO - massive amount of work around logging the variables and which ones are being picked up through the env vars
  (let [home (System/getProperty "user.home")
        public-config (-> "resources/config.edn" slurp edn/read-string)
        private-config (-> (str home "/.lein/structurize/config.edn") slurp edn/read-string)]
    (merge public-config private-config)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; component setup


(defrecord ConfigOpts []
  component/Lifecycle

  (start [component]
    (log/info "Initialising config-opts")
    (let [config (load-config)]
      (assoc component
             :general (general-config-opts config)
             :chsk-conn (chsk-conn-config-opts config)
             :handler (handler-config-opts config)
             :server (server-config-opts config))))

  (stop [component]
    (assoc component
           :general nil
           :chsk-conn nil
           :handler nil
           :server nil)))
