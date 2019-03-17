(ns myyoutube.views
  (:require-macros [myyoutube.utils :refer [defview letsubs]])
  (:require [myyoutube.components :as c]
            [myyoutube.youtube-api :as api]))

(defview subscriptions []
  (letsubs [api [:get :api]]
    (let [items (get-in api [:result :items])]
      [c/view {:flex-wrap :wrap}
       (for [item items]
         ^{:key item}
         [c/view {:margin-top 10} (get-in item [:snippet :title])])])))

(defview popular []
  (letsubs [api [:get :pop-api]]
    (let [items (get-in api [:result :items])]
      [c/view {:flex-wrap :wrap}
       (for [item items]
         ^{:key item}
         [c/view {:margin-top 10} (get-in item [:snippet :title])])])))

(defview main-view []
  (letsubs [signed-in? [:get :signed-in?]]
    [c/view {:flex 1}
     (when signed-in?
       [c/view {:flex-direction :row}
        [c/button {:on-press #(api/sing-out)} "Sign out"]])
     [c/view {:align-items :center :justify-content :center :flex 1}
      (if signed-in?
        [c/view
         [:div "signed"]
         [c/button {:on-press #(api/subscriptions)} "subscriptions"]
         [c/button {:on-press #(api/popular)} "popular"]
         [subscriptions]
         [popular]]
        [c/button {:on-press #(api/sing-in)} "Sign in!"])]]))