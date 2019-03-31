(ns myyoutube.ui.subs
  (:require [re-frame.core :as re-frame]
            [cljs-time.format :as format]
            [cljs-time.core :as time]))

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

(re-frame/reg-sub
 :popular-filtered
 (fn [[_ code] _]
   [(re-frame/subscribe [:get-api-by-code [:popular code]])
    (re-frame/subscribe [:storage/filter])])
 (fn [[popular filter] _]
   (if (empty? filter)
     popular
     (let [filter (set filter)]
       (remove #(filter (get-in % [:snippet :channelId])) popular)))))

(re-frame/reg-sub
 :subscriptions
 :<- [:api]
 (fn [api _]
   (get api :subscriptions)))

(re-frame/reg-sub
 :sorted-subscriptions
 :<- [:subscriptions]
 (fn [subscriptions _]
   (sort-by #(format/parse (format/formatters :date-time)
                           (get-in % [:snippet :publishedAt]))
            time/after?
            subscriptions)))

(re-frame/reg-sub
 :color
 :<- [:storage/bg]
 (fn [bg _]
   (if bg :black :white)))

(re-frame/reg-sub
 :oppo-color
 :<- [:storage/bg]
 (fn [bg _]
   (if bg :white :black)))