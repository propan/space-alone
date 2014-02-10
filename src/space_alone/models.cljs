(ns space-alone.models
  (:require [space-alone.constants :as C]
            [space-alone.utils :as u]))

(defrecord Asteroid [x y vX vY energy size type rotate rotation rotation-speed])

(defrecord Bullet [x y vX vY energy radius])

(defrecord Ship [x y vX vY thrust rotation rotate accelerate shoot next-shoot radius])

(defrecord GameScreen [background-image asteroids bullets ship next-asteroid lives score])

(defrecord WelcomeScreen [background-image asteroids])

(defn random-rotation
  []
  (let [direction (Math/random)]
    (cond
     (< direction 0.33) :left
     (< direction 0.66) :right
     :else              :none)))

(defn random-speed
  [size direction]
  (case size
    4 (* direction (u/random-float 0.01 0.3))
    3 (* direction (u/random-float 0.1  0.5))
    2 (* direction (u/random-float 0.2  0.7))
    1 (* direction (u/random-float 0.3  0.9))))

(defn x-dir
  [x]
  (cond
   (< x C/LEFT_EDGE) 1
   (> x C/RIGHT_EDGE) -1
   :else (if (< (Math/random) 0.5) -1 1)))

(defn y-dir
  [y]
  (cond
   (< y C/TOP_EDGE) -1
   (> y C/BOTTOM_EDGE) 1
   :else (if (< (Math/random) 0.5) -1 1)))

(defn asteroid
  [x y size]
  (Asteroid. x y (random-speed size (x-dir x))
                 (random-speed size (y-dir y))
                 (* size 30)
                 size
                 (u/random-int 1 4)
                 (random-rotation)
                 0
                 (u/random-float 0.1 0.6)))

(defn bullet
  [x y rotation]
  (let [vX (* C/BULLET_SPEED (Math/sin (* rotation (- C/RAD_FACTOR))))
        vY (* C/BULLET_SPEED (Math/cos (* rotation (- C/RAD_FACTOR))))]
    (Bullet. (- x vX) (- y vY) vX vY C/BULLET_ENERGY 5)))

(defn ship
  [x y]
  (Ship. x y 0 0 0 0 :none false false 0 15))

(defn game-screen
  []
  (GameScreen. (u/image "resources/images/background.jpg")
               [] []
               (ship (/ C/SCREEN_WIDTH 2)
                     (/ C/SCREEN_HEIGHT 2))
               (u/random-int C/MIN_TIME_BEFORE_ASTEROID
                             C/MAX_TIME_BEFORE_ASTEROID)
               3 0))

(defn welcome-screen
  []
  (WelcomeScreen. (u/image "resources/images/background.jpg")
                  (repeatedly 5 #(asteroid (u/random-int C/LEFT_EDGE C/RIGHT_EDGE)
                                           (u/random-int C/TOP_EDGE C/BOTTOM_EDGE) 4))))
