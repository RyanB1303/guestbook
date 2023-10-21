(ns guestbook.websockets
  (:require [cljs.reader :as edn]))

(defonce channel (atom nil))

(defn connect! [url recieve-handler]
  (if-let [chan (js/WebSocket. url)]
    (do
      (.log js/console "WebSocket connected")
      (set! (.-onmessage chan) #(->> %
                                     .-data
                                     edn/read-string
                                     recieve-handler))
      (reset! channel chan))
    (throw (ex-info "WebSocket connection failed"
                    {:url url}))))

(defn send-message! [msg]
  (if-let [chan @channel]
    (.send chan (pr-str msg))
    (throw (ex-info "Could'nt send message, channel isn't open!"
                    {:message msg}))))