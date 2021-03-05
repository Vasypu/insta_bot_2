import entity.Post;
import entity.User;
import org.apache.commons.io.FileUtils;
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
import java.io.InputStream;
import java.net.URL;

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
                GetFile file = new GetFile().setFileId(update.getMessage().getPhoto().get(0).getFileId());
                String filePath = null;
                try {
                    filePath = execute(file).getFilePath();
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                String downPath = "https://api.telegram.org/file/bot"+ getBotToken() + "/" + filePath;
                try {
                    HttpDownload.downloadFile(downPath, "./images", update.getMessage().getPhoto().get(0).getFileId() + ".jpg");
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }

                Post post = new Post();
                post.setTitle(update.getMessage().getCaption());
                post.setPhoto(new File("./images/" + update.getMessage().getPhoto().get(0).getFileId() + ".jpg").getPath());
                user.addPost(post);

                manager.persist(post);
                manager.persist(user);
            }
            manager.getTransaction().commit();
            manager.close();
    }

    @Override
    public String getBotUsername() { return "my_first_insta_bot"; }

    @Override
    public String getBotToken() { return "1691636958:AAHe-tQg-yxX-Vnl_l0SYApELubjjBRKbTU"; }
}