(ns space-alone.draw
  (:require-macros [space-alone.macros :refer [with-context]])
  (:require [space-alone.constants :as C]
            [space-alone.models :refer [Asteroid Bullet Ship GameScreen WelcomeScreen]]))

(defn- draw-text
  [ctx x y text]
  (let [dialog-width (.-width (.measureText ctx text))]
    (.fillText ctx text (- x (/ dialog-width 2)) y)))

;;
;;
;;

(defprotocol Drawable
  (draw [_ context]))

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

(extend-type GameScreen
  Drawable
  (draw [{:keys [ship bullets asteroids]} context]
    (.clearRect context 0 0 C/SCREEN_WIDTH C/SCREEN_HEIGHT)
    (doseq [b bullets]
      (draw b context))
    (doseq [a asteroids]
      (draw a context))
    (draw ship context)))

(extend-type WelcomeScreen
  Drawable
  (draw [screen context]
    (with-context [ctx context]
      (doto ctx
        (.clearRect 0 0 C/SCREEN_WIDTH C/SCREEN_HEIGHT)
        (aset "font" "80px Raleway")
        (aset "fillStyle" "#FFFFFF")
        (.translate (/ C/SCREEN_WIDTH 2) (/ C/SCREEN_HEIGHT 2))
        (draw-text 0 0 "SPACE ALONE")
        (aset "font" "14px Raleway")
        (aset "globalAlpha" (mod (.getSeconds (js/Date.)) 2))
        (draw-text 0 30 "press SPACE to start the game")))))
