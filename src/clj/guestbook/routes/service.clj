(ns guestbook.routes.service
  (:require
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.ring.coercion :as coercion]
   [reitit.coercion.spec :as spec-coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.exception :as exception]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.parameters :as parameters]
   [guestbook.messages :as msg]
   [guestbook.auth :as auth]
   [ring.util.http-response :as response]
   [guestbook.middleware.formats :as formats]))

(defn service-routes []
  ["/api"
   {:middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; exception handling
                 exception/exception-middleware
                 ;; coercing response body
                 coercion/coerce-response-middleware
                 ;; coercing request params
                 coercion/coerce-request-middleware
                 ;; multipart params (file)
                 multipart/multipart-middleware]
    :muuntaja formats/instance
    :coercion spec-coercion/coercion
    :swagger {:id ::api}}

   ["" {:no-doc true}
    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]
    ["/swagger-ui*"
     {:get (swagger-ui/create-swagger-ui-handler
            {:url "/api/swagger.json"})}]]
   ;; auth
   ["/login"
    {:post {:parameters
            {:body
             {:login string?
              :password string?}}
            :responses
            {200
             {:body
              {:identity
               {:login string?
                :created_at inst?}}}
             401
             {:body
              {:message string?}}}
            :handler
            (fn [{{{:keys [login password]} :body} :parameters session :session}]
              (if-some [user (auth/authenticate-user login password)]
                (-> (response/ok {:identity user})
                    (assoc :session (assoc session :identity user)))
                (response/unauthorized
                 {:message "Incorrect user or password"})))}}]
   ["/register"
    {:post
     {:parameters
      {:body
       {:login string?
        :password string?
        :confirm string?}}
      :responses
      {200
       {:body
        {:message string?}}
       400
       {:body
        {:message string?}}
       409
       {:body
        {:message string?}}}
      :handler
      (fn [{{{:keys [login password confirm]} :body} :parameters}]
        (if-not (= password confirm)
          (response/bad-request
           {:message "Password and Confirm do not match."})
          (try
            (auth/create-user! login password)
            (response/ok
             {:message "User registration successful. Please log in."})
            (catch clojure.lang.ExceptionInfo e
              (if (= (:guestbook/error-id (ex-data e))
                     ::auth/duplicate-user)
                (response/conflict
                 {:message "Registration failed! User with login already exists!"})
                (throw e))))))}}]
   ["/logout"
    {:post {:handler
            (fn [_] (->
                     (response/ok)
                     (assoc :session nil)))}}]
   ;; message / post
   ["/messages"
    {:get
     {:responses
      {200
       {:body ;; Data Spec for response body
        {:messages
         [{:id pos-int?
           :name string?
           :message string?
           :timestamp inst?}]}}}
      :handler
      (fn [_]
        (response/ok (msg/message-list)))}}]

   ["/message"
    {:post
     {:parameters
      {:body ;; Data Spec for request body parameters
       {:name string?
        :message string?}}
      :responses
      {200 {:body map?}
       400 {:body map?}
       500 {:errors map?}}
      :handler
      (fn [{{params :body} :parameters}]
        (try (msg/save-message! params)
             (response/ok {:status :ok})
             ;; if something bad happen
             (catch Exception e
               (let [{id :guestbook/error-id
                      errors :errors} (ex-data e)]
                 (case id
                   :validation ;; if id == validation response
                   (response/bad-request {:errors errors})
                    ;;else do
                   (response/internal-server-error
                    {:errors {:server-error ["Failed to save message!"]}}))))))}}]])
