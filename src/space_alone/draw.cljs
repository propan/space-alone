(ns space-alone.draw
  (:require-macros [space-alone.macros :refer [with-context]])
  (:require [space-alone.constants :as C]
            [space-alone.models :refer [Asteroid Bullet Ship]]))

(defprotocol Drawable
  (draw [this context]))

(extend-type Asteroid
  Drawable
  (draw [{:keys [x y size]} context]
    (with-context [ctx context]
      (doto ctx
        (.translate x y)
        (aset "strokeStyle" "purple")
        (aset "fillStyle" "purple")
        (.beginPath)
        (.arc 0 0 (size C/ASTEROID_SIZES) 0 (* 2 Math/PI) false)
        (.fill)
        (.closePath)))))

(extend-type Bullet
  Drawable
  (draw [{:keys [x y rotation]} context]
    (with-context [ctx context]
      (doto ctx
        (.translate x y)
        (aset "strokeStyle" "#FFFFFF")
        (aset "lineWidth" 3)
        (.beginPath)
        (.moveTo -1 0)
        (.lineTo 1 0)
        (.stroke)
        (.closePath)))))

(extend-type Ship
  Drawable
  (draw [{:keys [x y rotation]} context]
    (with-context [ctx context]
      (doto ctx
        (aset "strokeStyle" "blue")
        (aset "fillStyle" "blue")
        (.translate x y)
        (.rotate (* rotation C/RAD_FACTOR))
        (.beginPath)
        (.lineTo (/ C/SHIP_WIDTH -2) 0)
        (.lineTo 0 (- C/SHIP_HEIGHT))
        (.lineTo (/ C/SHIP_WIDTH 2) 0)
        (.closePath)
        (.fill)))))
