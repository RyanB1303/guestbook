(ns guestbook.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]))

(defn message-form
  []
  (let [fields (r/atom {})]
    (fn []
      [:div
       [:div.field
        [:label.label {:for :name} "Name"]
        [:input.input {:type :text
                       :name :name
                       :on-change #(swap! fields assoc :name (-> % .-target .-value))
                       :value (:name @fields)}]]
       [:div.field
        [:label.label {:for :message} "Message"]
        [:textarea.textarea {:name :message
                             :value (:message @fields)
                             :on-change #(swap! fields assoc :message (-> % .-target .-value))}]]
       [:input.button.is-primay {:type :submit
                                 :value "comment"}]
       [:div.mt-3
        [:p "Name: " (:name @fields)]
        [:p "Message: " (:message @fields)]]])))

(defn home []
  [:div.content>div.columns.is-centered>div.column.is-two-thirds
   [:div.columns>div.column
    [message-form]]])

(rdom/render
 [home]
 (.getElementById js/document "main"))
