(ns space-alone.draw
  (:require-macros [space-alone.macros :refer [with-context]])
  (:require [space-alone.constants :as C]
            [space-alone.models :refer [Asteroid Bullet ObjectPiece Particle Ship TextEffect
                                        GameScreen GameOverScreen WelcomeScreen]]))

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
  [ctx x y radius color]
  (doto (.createRadialGradient ctx x y 0 x y radius)
    (.addColorStop 0 "#FFFFFF")
    (.addColorStop 0.4 "#FFFFFF")
    (.addColorStop 0.4 color)
    (.addColorStop 1.0 "#000000")))

(defn- draw-stat-panel
  [context lives score]
  (with-context [ctx context]
    (doto ctx
      (aset "font" "16px Rammetto One")
      (.translate 25 30)
      (aset "fillStyle" "#FFFFFF")
      (draw-text 20 0 "LIVES" :center)
      (draw-text 105 0 "SCORE" :center)
      (aset "fillStyle" "#FF0000")
      (draw-text 60 0 (str lives) :center)
      (draw-text 145 0 (str score) :right))))

(defn- stroke-asteroid
  [ctx type]
  (let [points (get C/ASTEROID_POINTS type)
        [x y]  (first points)]
    (.moveTo ctx x y)
    (doseq [[x y] (rest points)]
      (.lineTo ctx x y))))

;;
;; Drawable Protocol
;;

(defprotocol Drawable
  (draw [_ context]))

(extend-type Asteroid
  Drawable
  (draw [{:keys [x y size type rotation]} context]
    (with-context [ctx context]
      (let [radius (* size C/ASTEROID_UNIT_SIZE)
            scale  (* 1.5 size)
            width  (* 3.0 (/ 1.5 size))]
        (doto ctx
          (aset "lineWidth" width)
          (aset "strokeStyle" "#C0C0C0")
          (.translate x y)
          (.scale scale scale)
          (.rotate (* rotation C/RAD_FACTOR))
          (.beginPath)
          (stroke-asteroid type)
          (.closePath)
          (.stroke)
          (.fill))))))

(extend-type Bullet
  Drawable
  (draw [{:keys [x y radius]} context]
    (with-context [ctx context]
      (doto ctx
        (aset "shadowBlur" C/SHADOW_BLUR)
        (aset "shadowColor" "#FF0000")
        (aset "fillStyle" (create-gradient ctx 0 0 radius "#FF0000"))
        (.translate x y)
        (.beginPath)
        (.arc 0 0 radius (* 2 Math/PI) false)
        (.fill)))))

(extend-type ObjectPiece
  Drawable
  (draw [{:keys [x y lx ly rx ry size rotation color]} context]
    (with-context [ctx context]
      (let [scale (* 1.5 size)
            width (* 1.0 (/ 1.5 size))]
        (doto ctx
          (aset "shadowBlur" 15)
          (aset "shadowColor" color)
          (aset "lineWidth" width)
          (aset "strokeStyle" color)
          (.translate x y)
          (.scale scale scale)
          (.rotate (* rotation C/RAD_FACTOR))
          (.beginPath)
          (.moveTo lx ly)
          (.lineTo rx ry)
          (.closePath)
          (.stroke))))))

(extend-type Particle
  Drawable
  (draw [{:keys [x y radius color lifespan]} context]
    (with-context [ctx context]
      (doto ctx
        (aset "globalCompositeOperation" "lighter")
        (aset "shadowBlur" C/SHADOW_BLUR)
        (aset "shadowColor" radius)
        (aset "fillStyle" (create-gradient ctx 0 0 radius color))
        (.translate x y)
        (.beginPath)
        (.arc 0 0 radius (* 2 Math/PI) false)
        (.fill)))))

(extend-type Ship
  Drawable
  (draw [{:keys [x y rotation]} context]
    (with-context [ctx context]
      (doto ctx
        (aset "shadowBlur" C/SHADOW_BLUR)
        (aset "shadowColor" C/SHIP_COLOR)
        (aset "strokeStyle" C/SHIP_COLOR)
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

(extend-type TextEffect
  Drawable
  (draw [{:keys [x y text scale color lifespan ticks-left]} context]
    (with-context [ctx context]
      (let [alfa (- 1 (/ ticks-left lifespan))]
        (doto ctx
          (aset "globalAlpha" alfa)
          (aset "shadowBlur" C/SHADOW_BLUR)
          (aset "shadowColor" color)
          (aset "fillStyle" color)
          (aset "font" "14px Rammetto One")
          (.translate x y)
          (.scale scale scale)
          (draw-text 0 0 text :center))))))

(extend-type GameScreen
  Drawable
  (draw [{:keys [background-image ship bullets asteroids effects lives score]} context]
    (.drawImage context background-image 0 0)
    (doseq [b bullets]
      (draw b context))
    (doseq [a asteroids]
      (draw a context))
    (draw ship context)
    (doseq [e effects]
      (draw e context))
    (draw-stat-panel context lives score)))

(extend-type GameOverScreen
  Drawable
  (draw [{:keys [background-image asteroids bullets effects score]} context]
    (.drawImage context background-image 0 0)
    (doseq [b bullets]
      (draw b context))
    (doseq [a asteroids]
      (draw a context))
    (doseq [e effects]
      (draw e context))
    (with-context [ctx context]
      (doto ctx
        (aset "font" "80px Rammetto One")
        (aset "fillStyle" "#FFFFFF")
        (.translate (/ C/SCREEN_WIDTH 2) (/ C/SCREEN_HEIGHT 2))
        (draw-text 0 0 "GAME OVER" :center)
        (aset "font" "22px Rammetto One")
        (aset "fillStyle" "#FF0000")
        (draw-text 0 40 (str "YOUR SCORE: " score) :center)
        (aset "fillStyle" "#FFFFFF")
        (aset "font" "14px Rammetto One")
        (aset "globalAlpha" (mod (.getSeconds (js/Date.)) 2))
        (draw-text 0 75 "press N to start the game" :center)))))

(extend-type WelcomeScreen
  Drawable
  (draw [{:keys [background-image asteroids]} context]
    (.drawImage context background-image 0 0)
    (doseq [a asteroids]
      (draw a context))
    (with-context [ctx context]
      (doto ctx
        (aset "font" "80px Rammetto One")
        (aset "fillStyle" "#FFFFFF")
        (.translate (/ C/SCREEN_WIDTH 2) (/ C/SCREEN_HEIGHT 2))
        (draw-text 0 0 "SPACE ALONE" :center)
        (aset "font" "14px Rammetto One")
        (aset "globalAlpha" (mod (.getSeconds (js/Date.)) 2))
        (draw-text 0 30 "press N to start the game" :center)))))
