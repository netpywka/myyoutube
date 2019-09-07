(ns myyoutube.ui.views
  (:require-macros [myyoutube.utils :refer [defview letsubs]])
  (:require [myyoutube.ui.components :as c]
            [myyoutube.youtube-api :as api]
            [re-frame.core :as re-frame]
            [myyoutube.ui.popup.views :as popup]
            [myyoutube.ui.items.views :as items]))

(defview filter-view []
  (letsubs [filter [:storage/filter]]
    [c/button {:on-press #(re-frame/dispatch [:set :settings-form {:type :filters :title "Blocked channels"}])
               :style    {:padding 2}}
     (count filter)]))

(defview quota-view [oppo-color]
  (letsubs [{:keys [number]} [:storage/quota]]
    (when number
      [c/view {:margin-left 20 :color oppo-color}
       "quota: " (int (- 100 (/ number 100))) "%"])))

(defn refresh-button []
  [c/view {:margin-left 10}
   [c/touchable {:on-press #(do
                              (re-frame/dispatch [:init-quota])
                              (re-frame/dispatch [:get-api]))}
    [:img {:src "./assets/refresh.svg" :width 16 :height 16}]]])

(defview main-view []
  (letsubs [client-id             [:storage/client-id]
            bg                    [:storage/bg]
            signed-in?            [:get :signed-in?]
            initialized?          [:get :initialized?]
            initialization-failed [:get :initialization-failed]
            color                 [:color]
            oppo-color            [:oppo-color]]
    [c/view {:flex 1 :padding-left 5 :padding-right 5 :background-color color :height "100%"}
     (if signed-in?
       [c/view {:flex 1 :height "100%"}
        [c/view {:flex-direction :row :align-items :center}
         [c/touchable {:on-press #(js/window.open "https://github.com/netpywka/myyoutube" "_blank")}
          [:img {:src "parsley.png" :height 30}]]
         [c/touchable {:on-press #(re-frame/dispatch [:set-in [:storage :bg] (not bg)])}
          [c/view {:flex        1 :background-color oppo-color :border-radius "4px" :padding 2 :color oppo-color
                   :margin-left 10}
           "C"]]
         [c/button {:on-press #(re-frame/dispatch [:set :settings-form {:type  :add-new
                                                                        :title "Add new"
                                                                        :data  {:type :popular :country "AF"}}])
                    :style    {:margin-left 20 :padding 2}} " + "]
         [c/view {:margin-left 10 :color oppo-color} "blocked: "]
         [filter-view]
         [quota-view oppo-color]
         [refresh-button]
         [c/view {:flex 1}]
         [c/button {:on-press #(api/sing-out) :style {:padding 2}} "Sign out"]]
        [items/items-view]]
       [c/view {:align-items :center :justify-content :center :flex 1}
        (if client-id
          (if initialized?
            [c/view {:flex-direction :row :align-items :center}
             [c/button {:on-press #(re-frame/dispatch [:refresh-client])} "Use another CLIENT ID"]
             [c/view {:padding-right 5 :padding-left 5} " or "]
             [c/button {:on-press #(api/sing-in)} "Sign in!"]]
            (if initialization-failed
              [c/view {:flex-direction :row :align-items :center}
               ;;js/gapi.client may be called only once, so we need to refresh a page
               [c/button {:on-press #(re-frame/dispatch [:refresh-client])} "Use another CLIENT ID"]
               [c/view {:padding-right 5 :padding-left 5} " or "]
               [c/button {:on-press #(re-frame/dispatch [:init-client])} "Init client"]]
              [:img {:src "parsley.png" :height 150}]))
          [:input {:style       {:width "500px"}
                   :type        :text
                   :placeholder "Paste Goodle API CLIENT ID here"
                   :on-change   #(do (re-frame/dispatch [:ls/store :client-id (.-value (.-target %))])
                                     (re-frame/dispatch [:set :initialization-failed true]))}])])
     [popup/popup]]))