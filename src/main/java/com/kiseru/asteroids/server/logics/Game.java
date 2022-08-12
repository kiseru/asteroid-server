package com.kiseru.asteroids.server.logics;

import com.kiseru.asteroids.server.User;
import com.kiseru.asteroids.server.handler.SpaceshipCrashHandler;
import com.kiseru.asteroids.server.handler.impl.SpaceshipCrashHandlerImpl;
import com.kiseru.asteroids.server.model.Asteroid;
import com.kiseru.asteroids.server.model.Coordinates;
import com.kiseru.asteroids.server.model.Garbage;
import com.kiseru.asteroids.server.model.Point;
import com.kiseru.asteroids.server.model.Screen;
import com.kiseru.asteroids.server.model.Spaceship;
import com.kiseru.asteroids.server.service.impl.CourseCheckerServiceImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Bulat Giniyatullin
 * 08 Декабрь 2017
 */

public class Game {

    private static final Random random = new Random();

    private final List<Point> gameObjects = new ArrayList<>();

    private final List<Point> pointsOnScreen = new ArrayList<>();

    private final Screen screen;

    private final List<SpaceshipCrashHandler> crashHandlers = new ArrayList<>();

    private final AtomicInteger collectedGarbageCount = new AtomicInteger(0);

    private final int asteroidNumber;

    private final int garbageNumber;

    /**
     * Конструктор создания игровой сессии
     *
     * @param screen         - экран
     * @param garbageNumber  - количество мусора для первоначальной генерации
     * @param asteroidNumber - количество астероидов для первоначальной генерации
     */
    public Game(Screen screen, int garbageNumber, int asteroidNumber) {
        this.garbageNumber = garbageNumber;
        this.screen = screen;
        this.asteroidNumber = asteroidNumber;
        init();
    }

    /**
     * создает и регистрирует новый корабль
     *
     * @param user - юзер, для которого регистриуется корабль
     */
    public void registerSpaceshipForUser(User user) {
        var courseCheckerService = new CourseCheckerServiceImpl(pointsOnScreen, screen);
        Spaceship spaceship = new Spaceship(user, courseCheckerService, generateUniqueRandomCoordinates());
        courseCheckerService.setSpaceship(spaceship);
        pointsOnScreen.add(spaceship);
        gameObjects.add(spaceship);
        crashHandlers.add(new SpaceshipCrashHandlerImpl(this, spaceship));
        user.setSpaceship(spaceship);
    }

    /**
     * запускает и поддерживает жизненный цикл игры
     */
    public void refresh() {
        screen.update();
        crashHandlers.forEach(SpaceshipCrashHandler::check);
        gameObjects.forEach(o -> o.render(screen));
    }

    private Coordinates generateUniqueRandomCoordinates() {
        Coordinates randomCoordinates = generateCoordinates();
        while (isGameObjectsContainsCoordinates(randomCoordinates)) {
            randomCoordinates = generateCoordinates();
        }
        return randomCoordinates;
    }

    private Coordinates generateCoordinates() {
        return new Coordinates(random.nextInt(screen.getWidth()) + 1,
                               random.nextInt(screen.getHeight()) + 1);
    }

    private void init() {
        generateGarbage();
        generateAsteroids();
    }

    private void generateAsteroids() {
        for (int i = 0; i < asteroidNumber; i++) {
            Asteroid asteroid = new Asteroid(generateUniqueRandomCoordinates());
            pointsOnScreen.add(asteroid);
            gameObjects.add(asteroid);
        }
    }

    private void generateGarbage() {
        for (int i = 0; i < garbageNumber; i++) {
            Garbage garbage = new Garbage(generateUniqueRandomCoordinates());
            pointsOnScreen.add(garbage);
            gameObjects.add(garbage);
        }
    }

    private boolean isGameObjectsContainsCoordinates(Coordinates coordinates) {
        return pointsOnScreen.stream()
                .anyMatch(p -> p.getCoordinates().equals(coordinates));
    }

    public void showField() {
        screen.display();
    }

    public List<Point> getPointsOnScreen() {
        return pointsOnScreen;
    }

    public Screen getScreen() {
        return screen;
    }

    public int getGarbageNumber() {
        return garbageNumber;
    }

    public int incrementCollectedGarbageCount() {
        return collectedGarbageCount.incrementAndGet();
    }
}