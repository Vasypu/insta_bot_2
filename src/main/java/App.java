import database.DatabaseConnector;
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

import javax.persistence.EntityManagerFactory;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class App extends TelegramLongPollingBot {
    private DatabaseConnector connector;

    public App(DatabaseConnector connector) { this.connector = connector; }

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    @Override
    public void onUpdateReceived(Update update) {
        Long userId = update.getMessage().getChatId();
        connector.startTransaction();
        User user = connector.getUserService().findById(userId);
        System.out.println("Message " + update.getMessage().getText());
        System.out.println("Caption " + update.getMessage().getCaption());
        if (user == null) {
            try {
                execute(new SendMessage(update.getMessage().getChatId(),
                        "Вам необходимо прислать логин и пароль в одном предложении через пробел"));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            connector.getUserService().save(new User(update.getMessage().getChatId(), null, null));
        } else if (user.getLogin() == null || user.getPassword() == null) { // запись логина и пароля

            String[] loginAndPassword = update.getMessage().getText().split(" ");
            user.setLogin(loginAndPassword[0]);
            user.setPassword(loginAndPassword[1]);

            connector.getUserService().save(user);

            try {
                execute(new SendMessage(update.getMessage().getChatId(),
                        "Все работает! Теперь вы можете присылать нам текст, изображение и дату публикации" +
                                "(формат: чч:мм дд:ММ.гггг, в начале изображения) для Instagram (в одном сообщении)"));
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

            SimpleDateFormat parser = new SimpleDateFormat("hh:mm dd.MM.yyyy");
            Date datePost = null;
            try {
                datePost = parser.parse(update.getMessage().getCaption().split("\n")[0]);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            Runnable sendToInstagram = () -> {
                System.out.println("Hello from scheduler");
                // здесь необходимо подключить библиотек Instagram4j
            };

            scheduler.schedule(sendToInstagram, datePost.getTime() - System.currentTimeMillis(),
                    TimeUnit.MILLISECONDS);

            Post post = new Post();
            post.setDate(datePost);
            post.setTitle(update.getMessage().getCaption().replace(update.getMessage().getCaption().split("\n")[0] + "\n",
                    ""));
            post.setPhoto(new File("./images/" + update.getMessage().getPhoto().get(0).getFileId() + ".jpg").getPath());
            user.addPost(post);

            connector.getPostService().save(post);
            connector.getUserService().save(user);
        }
        connector.endTransaction();
    }

    @Override
    public String getBotUsername() { return "second_insta_bot"; }

    @Override
    public String getBotToken() { return "1599893568:AAG3q_WkZZnTmV5sJRTIUp3BHsnE8XsrHwE"; }
}