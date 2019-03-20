(ns myyoutube.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :get
 (fn [db [_ k]]
   (get db k)))

(re-frame/reg-sub
 :get-in
 (fn [db [_ path]]
   (get-in db path)))

(re-frame/reg-sub :api :api)

(re-frame/reg-sub
 :get-api-by-code
 :<- [:api]
 (fn [api [_ path]]
   (get-in api path)))

(re-frame/reg-sub :filter :filter)

(re-frame/reg-sub
 :popular-filtered
 (fn [[_ code] _]
   [(re-frame/subscribe [:get-api-by-code [:popular code]])
    (re-frame/subscribe [:filter])])
 (fn [[popular filter] _]
   (if (empty? filter)
     popular
     (let [filter (set filter)]
       (remove #(filter (get-in % [:snippet :channelId])) popular)))))