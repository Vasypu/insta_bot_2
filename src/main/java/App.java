import entity.Post;
import entity.User;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.File;
import java.io.IOException;

public class App extends TelegramLongPollingBot {
    private static EntityManagerFactory entityManagerFactory;

    public App(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public static void main(String[] args) throws TelegramApiRequestException {
        entityManagerFactory = Persistence.createEntityManagerFactory("instabot");

        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        telegramBotsApi.registerBot(new App(entityManagerFactory));
//         выгрузка настроек
//        TelegramBot bot = new TelegramBot("1691636958:AAHe-tQg-yxX-Vnl_l0SYApELubjjBRKbTU");
//
//        bot.setUpdatesListener(updates -> {
//            updates.forEach(System.out::println);
//
//            updates.forEach(update -> {
//                Integer userId = update.message().from().id();
//                EntityManager manager = entityManagerFactory.createEntityManager();
//                manager.getTransaction().begin();
//                User user = manager.find(User.class, userId);
//                System.out.println("Photo " + update.message().photo());
//                if (user == null) {
//                    bot.execute(new SendMessage(update.message().chat().id(),
//                            "Вам необходимо прислать логин и пароль в одном предложении через пробел"));
//                    manager.persist(new User(update.message().from().id(), null, null));
//                } else if (user.getLogin() == null || user.getPassword() == null) { // запись логина и пароля
//
//                    String[] loginAndPassword = update.message().text().split(" ");
//                    user.setLogin(loginAndPassword[0]);
//                    user.setPassword(loginAndPassword[1]);
//
//                    manager.persist(user);
//
//                    bot.execute(new SendMessage(update.message().chat().id(),
//                            "Все работает! Теперь вы можете присылать нам текст/изображение для " +
//                                    "Instagram (в одном сообщении)"));
//
//                } else if (user.getLogin() != null && update.message().photo().length > 0) {
//
//                    GetFileResponse fileResponse = bot.execute(new GetFile(update.message().photo()[0].fileId()));
//                    String fullPath = bot.getFullFilePath(fileResponse.file());
//                    try {
//                        HttpDownload.downloadFile(fullPath, "./images", update.message().photo()[0].fileId() + ".jpg");
//                    } catch (IOException e) {
//                        System.err.println(e.getMessage());
//                    }
//
//                    Post post = new Post();
//                    post.setTitle(update.message().caption());
//                    post.setPhoto(new File("./images/" + update.message().photo()[0].fileId() + ".jpg").getPath());
//                    user.addPost(post);
//
//                    manager.persist(post);
//                    manager.persist(user);
//                }
//                manager.getTransaction().commit();
//                manager.close();
//            });
//
//            return UpdatesListener.CONFIRMED_UPDATES_ALL;
//        });
    }

    @Override
    public void onUpdateReceived(Update update) {
            Long userId = update.getMessage().getChatId();
            EntityManager manager = entityManagerFactory.createEntityManager();
            manager.getTransaction().begin();
            User user = manager.find(User.class, userId);
            System.out.println("Message " + update.getMessage().getText());
            if (user == null) {
                try {
                    execute(new SendMessage(update.getMessage().getChatId(),
                            "Вам необходимо прислать логин и пароль в одном предложении через пробел"));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                manager.persist(new User(update.getMessage().getChatId(), null, null));
            } else if (user.getLogin() == null || user.getPassword() == null) { // запись логина и пароля

                String[] loginAndPassword = update.getMessage().getText().split(" ");
                user.setLogin(loginAndPassword[0]);
                user.setPassword(loginAndPassword[1]);

                manager.persist(user);

                try {
                    execute(new SendMessage(update.getMessage().getChatId(),
                            "Все работает! Теперь вы можете присылать нам текст/изображение для Instagram (в одном сообщении)"));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (user.getLogin() != null && update.getMessage().getPhoto().size() > 0) {
                System.out.println("Photo " + update.getMessage().getPhoto().get(0).getFilePath());
                GetFile file = new GetFile();
                file.setFileId(update.getMessage().getPhoto().get(0).getFileId());
                String filePath = null;
                try {
                    filePath = execute(file).getFilePath();
                    System.out.println("Photo path " + file.getFileId());
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
//                File outputFile = new File("./images", update.getMessage().getPhoto() + ".jpg");
//                try {
//                    File file1 = downloadFile(filePath, outputFile);
//                } catch (TelegramApiException e) {
//                    e.printStackTrace();
//                }
//                File file = downloadFile(filePath, outputFile);

//                try {
//                    GetFileResponse fileResponse = execute(file.getMethod());
//                } catch (TelegramApiException e) {
//                    e.printStackTrace();
//                }
//                String fullPath = bot.getFullFilePath(fileResponse.file());

//                try {
//                    downloadFile(filePath);
//                } catch (TelegramApiException e) {
//                    e.printStackTrace();
//                }
                try {
                    System.out.println("Photo path " + filePath);
                    HttpDownload.downloadFile(filePath, "./images", update.getMessage().getPhoto() + ".jpg");
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }

                Post post = new Post();
                post.setTitle(update.getMessage().getCaption());
                post.setPhoto(new File("./images/" + update.getMessage().getPhoto() + ".jpg").getPath());
                user.addPost(post);

                manager.persist(post);
                manager.persist(user);
            }
            manager.getTransaction().commit();
            manager.close();
//        });
    }

    @Override
    public String getBotUsername() { return "my_first_insta_bot"; }

    @Override
    public String getBotToken() { return "1691636958:AAHe-tQg-yxX-Vnl_l0SYApELubjjBRKbTU"; }
}