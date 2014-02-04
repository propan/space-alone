(ns space-alone.constants)

(def RAD_FACTOR (/ Math.PI 180))

(def SCREEN_WIDTH 1024)
(def SCREEN_HEIGHT 600)

;; ship dimensions
(def SHIP_WIDTH 14)
(def SHIP_HEIGHT 28)

;; rewards
(def REWARDS {:large  150
              :medium 75
              :small  15})

;; movement constants
(def MAX_VELOCITY 6)
(def MAX_THRUST 2)

(def THRUST_DECLINE 0.3)

(def TURN_FACTOR 1.5)
(def ACCELERATION 0.01)

;; shooting constants
(def TIME_BETWEEN_SHOOTS 25)
(def BULLET_SPEED 15)
(def BULLET_ENERGY 35)

;; asteroids generation
(def MIN_TIME_BEFORE_ASTEROID 250)
(def MAX_TIME_BEFORE_ASTEROID 500)

(def ASTEROID_POWERS {:large  80
                      :medium 50
                      :small  20})

(def ASTEROID_SIZES {:large  45
                     :medium 30
                     :small  15})

;; controls
(def SPACE 32)

(def ARROW_LEFT 37)
(def ARROW_UP 38)
(def ARROW_RIGHT 39)
(def ARROW_DOWN 40)
