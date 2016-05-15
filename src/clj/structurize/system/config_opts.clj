(ns structurize.system.config-opts
  (:require [clojure.edn :as edn]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))


(defn load-config []
  (let [home (System/getProperty "user.home")
        public-config (-> "resources/config.edn" slurp edn/read-string)
        private-config (try
                         (-> (str home "/.lein/structurize/config.edn") slurp edn/read-string)
                         (catch java.io.FileNotFoundException e {}))]
    (merge public-config private-config)))


(defrecord ConfigOpts []
  component/Lifecycle

  (start [component]
    (log/info "initialising config-opts")
    (let [config (load-config)]
      (assoc component
             :github-auth {:client-id (:github-auth-client-id config)
                           :client-secret (or (System/getenv "GITHUB_AUTH_CLIENT_SECRET") (:github-auth-client-secret config))
                           :scope "user:email"}

             :comms {:chsk-opts {:packer :edn}}

             :server {:http-kit-opts {:port (or (System/getenv "PORT") (:port config))}
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
                                                    :default-charset "utf-8"}}})))

  (stop [component] component))
