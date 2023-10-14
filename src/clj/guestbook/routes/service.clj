(ns guestbook.routes.service
  (:require
   [guestbook.messages :as msg]
   [guestbook.middleware :as middleware]
   [ring.util.http-response :as response]))


(defn service-routes []
  ["/api"
   {:middleware [middleware/wrap-formats]}
   ["/messages"
    {:get (fn [_] 
            (response/ok (msg/message-list)))}]
   ["/message" 
    {:post (fn [{:keys [params]}] 
             (try (msg/save-message! params)
                  (response/ok {:status :ok})
                  (catch Exception e
                    (let [{id :guestbook/error-id
                           errors :errors} (ex-data e)]
                      (case id
                        :validation ;; if id == validation do
                        (response/bad-request {:errors errors})
                        ;;else do
                        (response/internal-server-error
                         {:errors {:server-error ["Failed to save message!"]}}))))))}]])