(ns myyoutube.components)

(defn view [& items]
  (let [styles? (map? (first items))]
    (vec (concat [:div {:style (merge {:display :flex :flex-direction :column} (when styles? (first items)))}]
                 (vec (if styles? (rest items) items))))))

(defn button [{:keys [on-press]} label]
  [:div {:style {:background-color :lightblue :cursor :pointer :display :flex :flex-direction :column}
         :on-click on-press} label])