(ns myyoutube.views
  (:require-macros [myyoutube.utils :refer [defview letsubs]])
  (:require [myyoutube.components :as c]
            [myyoutube.youtube-api :as api]))

(defview popular [code]
  (letsubs [items [:get-in [:api :popular code]]]
    [c/view {:margin-right 10}
     [:h2 "Popular ("code")"]
     [c/view {:overflow-y :auto}
      (for [{:keys [snippet id]} items]
        ^{:key snippet}
        [c/touchable {:on-press #(.open js/window (str "https://www.youtube.com/watch?v=" id) "_blank")}
         [:div {:style {:margin 0 :margin-top 10}}
          [:img {:src (get-in snippet [:thumbnails :medium :url]) :width 320 :height 180}]
          [:div {:style {:display :flex :font-size 13 :max-width 320}} (:title snippet)]]])]]))

(defview main-view []
  (letsubs [signed-in? [:get :signed-in?]]
    [c/view {:flex 1 :margin 10}
     (if signed-in?
       [c/view {:flex 1}
        [c/view {:flex-direction :row}
         [c/button {:on-press #(api/sing-out)} "Sign out"]]
        [c/view {:flex-direction :row :flex 1}
         [popular "RU"]
         [popular "US"]]]
       [c/view {:align-items :center :justify-content :center :flex 1}
          [c/button {:on-press #(api/sing-in)} "Sign in!"]])]))