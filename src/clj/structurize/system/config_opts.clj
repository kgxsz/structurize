(ns structurize.system.config-opts
  (:require [clojure.edn :as edn]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [camel-snake-kebab.core :as csk]))


(defn load-config
  "Loads pubic and private config from hardocded locations, then merges them.
   If no private file is present, will simply give an empty map in its place."
  []

  (let [home (System/getProperty "user.home")
        public-config (-> "resources/config.edn" slurp edn/read-string)
        private-config (try
                         (-> (str home "/.lein/structurize/config.edn") slurp edn/read-string)
                         (catch java.io.FileNotFoundException e {}))]
    (merge public-config private-config)))


(defn make-config
  "Returns a function that takes a kw and first checks if an env var exists,
   before going to the config map to get a value."
  []

  (let [config (load-config)]
    (fn [kw {:keys [type]}]
      (if-let [v (-> kw name csk/->SCREAMING_SNAKE_CASE System/getenv)]
        (case type
          :integer (edn/read-string v)
          v)
        (get config kw)))))


(defrecord ConfigOpts []
  component/Lifecycle

  (start [component]
    (log/info "initialising config-opts")
    (let [config (make-config)]
      (assoc component
             :github-auth {:client-id (config :github-auth-client-id {})
                           :client-secret (config :github-auth-client-secret {})
                           :redirect-prefix (config :github-auth-redirect-prefix {})
                           :scope "user:email"}

             :comms {:chsk-opts {:packer :edn}}

             :server {:http-kit-opts {:port (config :port {:type :integer})}
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
