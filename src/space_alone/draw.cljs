(ns space-alone.draw
  (:require-macros [space-alone.macros :refer [with-context]])
  (:require [space-alone.constants :as C]
            [space-alone.models :refer [Asteroid Bullet Ship GameScreen WelcomeScreen]]))

;
; Helpers
;

(defn- draw-text
  [ctx x y text align]
  (let [dialog-width (.-width (.measureText ctx text))]
    (case align
      :left  (.fillText ctx text (- x dialog-width) y)
      :right (.fillText ctx text x y)
             (.fillText ctx text (- x (/ dialog-width 2)) y))))

(defn- create-gradient
  [ctx x y]
  (doto (.createRadialGradient ctx x y 0 x y 5)
    (.addColorStop 0 "#FFFFFF")
    (.addColorStop 0.4 "#FFFFFF")
    (.addColorStop 0.4 "#FF0000")
    (.addColorStop 1.0 "#000000")))

(defn- draw-stat-panel
  [context lives score]
  (with-context [ctx context]
    (doto ctx
      (aset "font" "16px Helvetica")
      (.translate 25 30)
      (aset "fillStyle" "#FFFFFF")
      (draw-text 20 0 "LIVES" :center)
      (draw-text 105 0 "SCORE" :center)
      (aset "fillStyle" "#00FF00")
      (draw-text 60 0 (str lives) :center)
      (draw-text 145 0 (str score) :right))))

(defn- stroke-asteroid
  [ctx type]
  (case type
    1 (doto ctx
        (.moveTo 1 10)
        (.lineTo 7 8)
        (.lineTo 9 -4)
        (.lineTo 6 -5)
        (.lineTo -4 -4)
        (.lineTo -7 -7)
        (.lineTo -10 0)
        (.lineTo -10 5))
    2 (doto ctx
        (.moveTo 0 9)
        (.lineTo 6 8)
        (.lineTo 4 -3)
        (.lineTo -2 -8)
        (.lineTo -3 -7)
        (.lineTo -5 -7)
        (.lineTo -9 4)
        (.lineTo -4 9))
    3 (doto ctx
        (.moveTo 2 8)
        (.lineTo 6 10)
        (.lineTo 10 -4)
        (.lineTo 5 -3)
        (.lineTo 6 -6)
        (.lineTo 0 -10)
        (.lineTo -10 -4)
        (.lineTo -10 6)
        (.lineTo -4 9))
    4 (doto ctx
        (.moveTo 10 -3)
        (.lineTo 5 -10)
        (.lineTo -2 -8)
        (.lineTo -5 -10)
        (.lineTo -10 -5)
        (.lineTo -8 1)
        (.lineTo -8 10)
        (.lineTo 7 9))))

;;
;; Drawable Protocol
;;

(defprotocol Drawable
  (draw [_ context]))

(extend-type Asteroid
  Drawable
  (draw [{:keys [x y size type]} context]
    (with-context [ctx context]
      (let [a-size (size C/ASTEROID_SIZES)
            scale  (* 0.1 a-size)
            width  (/ 30 a-size)]
        (doto ctx
          (aset "lineWidth" width)
          (aset "strokeStyle" "#FFFFFF")
          (.translate x y)
          (.scale scale scale)
          (.beginPath)
          (stroke-asteroid type)
          (.closePath)
          (.stroke))))))

(extend-type Bullet
  Drawable
  (draw [{:keys [x y rotation]} context]
    (with-context [ctx context]
      (doto ctx
        (aset "shadowBlur" C/SHADOW_BLUR)
        (aset "shadowColor" "#FF0000")
        (aset "fillStyle" (create-gradient ctx 0 0))
        (.translate x y)
        (.rotate (* rotation C/RAD_FACTOR))
        (.beginPath)
        (.arc 0 0 5 (* 2 Math/PI) false)
        (.fill)))))

(extend-type Ship
  Drawable
  (draw [{:keys [x y rotation]} context]
    (with-context [ctx context]
      (doto ctx
        (aset "shadowBlur" C/SHADOW_BLUR)
        (aset "shadowColor" "#0000FF")
        (aset "strokeStyle" "#0000FF")
        (aset "lineWidth" 2.5)
        (.translate x y)
        (.rotate (* rotation C/RAD_FACTOR))
        (.beginPath)
        (.moveTo -10 10)
        (.lineTo 0 -15)
        (.lineTo 10 10)
        (.moveTo 7 5)
        (.lineTo -7 5)
        (.closePath)
        (.stroke)))))

(extend-type GameScreen
  Drawable
  (draw [{:keys [ship bullets asteroids lives score]} context]
    (.clearRect context 0 0 C/SCREEN_WIDTH C/SCREEN_HEIGHT)
    (doseq [b bullets]
      (draw b context))
    (doseq [a asteroids]
      (draw a context))
    (draw ship context)
    (draw-stat-panel context lives score)))

(extend-type WelcomeScreen
  Drawable
  (draw [screen context]
    (with-context [ctx context]
      (doto ctx
        (.clearRect 0 0 C/SCREEN_WIDTH C/SCREEN_HEIGHT)
        (aset "font" "80px Raleway")
        (aset "fillStyle" "#FFFFFF")
        (.translate (/ C/SCREEN_WIDTH 2) (/ C/SCREEN_HEIGHT 2))
        (draw-text 0 0 "SPACE ALONE" :center)
        (aset "font" "14px Raleway")
        (aset "globalAlpha" (mod (.getSeconds (js/Date.)) 2))
        (draw-text 0 30 "press SPACE to start the game" :center)))))
