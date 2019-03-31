(ns myyoutube.ui.popup.views
  (:require-macros [myyoutube.utils :refer [defview letsubs]])
  (:require [myyoutube.ui.components :as c]
            [re-frame.core :as re-frame]
            [clojure.string :as string]
            [myyoutube.contries :as contries]))

(defn add-popular []
  [c/view {:margin-top 10}
   [:select {:on-change #(re-frame/dispatch [:set-in [:settings-form :data :country] (.-value (.-target %))])}
    contries/contries-views]])

(defview add-new-view [{:keys [type]}]
  (letsubs [oppo-color [:oppo-color]]
    [c/view {:margin 30}
     [c/view
      [c/view {:flex-direction :row :align-items :center :justify-content :space-between}
       [c/view {:color oppo-color} "Add new"]]
      [c/view {:margin-top 10}
       [:select {:on-change #(re-frame/dispatch [:set-in [:settings-form :data :type] (keyword (.-value (.-target %)))])}
        [:option {:value :popular} "Popular"]
        [:option {:value :subscriptions} "Subscriptions"]
        [:option {:value :custom} "Custom"]]]
      (case type
        :popular [add-popular]
        [:div])
      [c/button {:on-press #(re-frame/dispatch [:add-new])
                 :style    {:margin-top 30 :justify-content :center}} "Add"]]]))

(defview edit-item [{:keys [type country]}]
  (letsubs [oppo-color [:oppo-color]]
    [c/view {:margin 30}
     [c/view
      [c/view {:flex-direction :row :align-items :center :justify-content :space-between}
       [c/view {:color oppo-color} "Edit"]]
      [c/view {:color oppo-color} (if (= :popular type) (str "Popular " country) "Subscriptions")]
      [c/view {:margin-top 10}]
      [c/button {:on-press #(re-frame/dispatch [:delete-item])
                 :style    {:margin-top 30 :justify-content :center}} "Delete"]]]))

(defview popup []
  (letsubs [{:keys [type data] :as form} [:get :settings-form]
            filter [:storage/filter]
            color  [:color]]
    (when form
      [c/view {:position    :absolute :left 0 :right 0 :top 0 :bottom 0 :background-color "rgba(0, 0, 0, 0.4)"
               :align-items :center :justify-content :center}
       [c/view {:background-color color :border-radius 4}
        [c/view {:justify-content :flex-end :flex-direction :row}
         [c/button {:on-press #(re-frame/dispatch [:set :settings-form nil])} "x"]]
        (case type
          :filters
          [c/view {:padding 20}
           [:textarea {:default-value (string/join " " filter)
                       :style         {:width 400 :height 300}
                       :on-change     #(re-frame/dispatch [:set-in [:settings-form :data :filter] (.-value (.-target %))])}]
           (when data
             [c/button {:on-press #(re-frame/dispatch [:save-filters]) :style {:width 150 :margin-top 10}} "Save"])]
          :add-new
          [add-new-view data]
          :edit-item
          [edit-item data])]])))