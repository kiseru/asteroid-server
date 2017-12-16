package com.tutorteam.server.room;

import com.tutorteam.logics.Game;
import com.tutorteam.logics.Screen;
import com.tutorteam.server.Server;
import com.tutorteam.server.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.IntStream;

final public class Room extends Thread {

    private ArrayList<User> users;
    private int usersCount;
    private RoomStatus roomStatus;
    private Game game;

    public Room() {
        users = new ArrayList<>();
        IntStream.iterate(0, i -> i + 1)
                .limit(5)
                .forEach(i -> users.add(null));
        usersCount = 0;
        roomStatus = RoomStatus.WAITING_CONNECTIONS;
    }

    public synchronized void addUser(User user) {
        if (usersCount >= 5) Server.getNotFullRoom().addUser(user);

        int emptyPlaceIndex = IntStream.iterate(0, index -> index + 1)
                .limit(users.size())
                .filter(index -> users.get(index) == null)
                .findFirst()
                .orElse(-1);

        if (emptyPlaceIndex == -1) return;

        users.stream()
                .filter(Objects::nonNull)
                .forEach(roomUser -> roomUser.sendMessage(String.format("User %s has joined the room.", user.getUserName())));

        users.set(emptyPlaceIndex, user);
        usersCount++;
    }

    public synchronized void removeUser(User user) {
        if (usersCount == 0) return;

        User removingUser = users.stream()
                .filter(user::equals)
                .findFirst()
                .orElse(null);

        if (removingUser == null) return;

        users.remove(removingUser);

        usersCount--;
    }

    public String getRating() {
        return users.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(User::getScore))
                .map(User::toString)
                .reduce("", (acc, userRow) -> acc + userRow + "\n");
    }

    public boolean isFull() {
        return usersCount == 5;
    }

    public boolean isGameStarted() {
        return roomStatus == RoomStatus.GAMING;
    }

    public boolean isGameFinished() {
        return roomStatus == RoomStatus.FINISHED;
    }

    @Override
    public void run() {
        users.stream()
                .filter(Objects::nonNull)
                .forEach(user -> user.sendMessage("Game has started!"));

        roomStatus = RoomStatus.GAMING;

        synchronized (Server.class) {
            game = new Game(new Screen(10, 10), 15, 15);
            users.stream()
                    .filter(Objects::nonNull)
                    .forEach(game::registerSpaceShipForUser);
            Server.class.notifyAll();
        }

        synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            roomStatus = RoomStatus.FINISHED;
        }

        users.stream()
                .filter(Objects::nonNull)
                .forEach(user -> user.sendMessage(this.getRating()));
    }

    public Game getGame() {
        return game;
    }
}
