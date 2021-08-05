package on.time;

import io.reactivex.Completable;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import on.time.auth.UserAuth;
import on.time.db.OnTimeClientStore;
import on.time.db.OnTimeDB;
import on.time.db.OnTimeTareaStore;
import on.time.routes.RutaCliente;
import on.time.routes.RutaTarea;

public class Main extends AbstractVerticle {
    public static Logger logger = LoggerFactory.getLogger(Main.class);

    @Override
    public Completable rxStart() {
        OnTimeDB db = OnTimeDB.getInstance();
        OnTimeClientStore clientStore = new OnTimeClientStore(db);
        OnTimeTareaStore tareaStore = new OnTimeTareaStore(db);

        UserAuth userAuth = new UserAuth(clientStore);

        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        router.route().method(HttpMethod.GET).path("/").handler(ctx -> {
            ctx.response().end("Bienvenido a nuestro proyecto!\n" +
                    "Este servidor no tiene un frontend actualmente :c\n" +
                    "Pero aun asi puedes hittearlo con curl o la mas cool httpie!");
        });

        RutaCliente rutaUsuarios = new RutaCliente(vertx, clientStore);
        router.mountSubRouter("/api/v1/usuarios", rutaUsuarios.getRouter());

        // Set up basic auth for certain routes
        router.route("/api/v1/tareas/*").handler(userAuth::parseBasicHttpAuth);

        RutaTarea rutaTareas = new RutaTarea(vertx, tareaStore);
        router.mountSubRouter("/api/v1/tareas", rutaTareas.getRouter());

        return vertx.createHttpServer()
                .requestHandler(router)
                .rxListen(Integer.parseInt(System.getProperty("server.port")))
                .doOnSuccess(server -> logger.info("Server started on port " + server.actualPort()))
                .doOnError(err -> logger.error("{}", err))
                .ignoreElement();
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new Main(), res -> {
            if (res.succeeded()) logger.info("Main verticle deployed");
            else logger.error("{}", res.cause());
        });
    }
}
