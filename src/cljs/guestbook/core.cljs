(ns guestbook.core)

(-> (.getElementById js/document "main")
    (.-innerHTML)
    (set! "Hello, World"))
