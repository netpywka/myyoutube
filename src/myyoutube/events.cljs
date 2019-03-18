(ns myyoutube.events
  (:require [re-frame.core :as re-frame]
            [myyoutube.youtube-api :as api]))

(re-frame/reg-event-db
 :initialize-db
 (fn [_ _]
   {:signed-in? false}))

(re-frame/reg-event-db
 :set
 (fn [db [_ k v]]
   (assoc db k v)))

(re-frame/reg-event-db
 :set-in
 (fn [db [_ path v]]
   (assoc-in db path v)))

(re-frame/reg-fx
 :api-get-popilar
 (fn []
    (println "POP")
    (api/popular "RU")
    (api/popular "US")))

(re-frame/reg-fx
 :api-get-subscriptions
 (fn []
   (api/subscriptions)))

(re-frame/reg-event-fx
 :get-api
 (fn [_ _]
   {:api-get-popilar       nil
    :api-get-subscriptions nil}))
