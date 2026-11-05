/*
 * This file is part of Aliucord, an Android Discord client mod.
 * Copyright (c) 2021 Juby210 & Vendicated
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.coreplugins.slashcommandsfix;

import android.content.Context;
import android.util.Base64;

import com.aliucord.api.GatewayAPI;
import com.aliucord.Http;
import com.aliucord.Logger;
import com.aliucord.patcher.InsteadHook;
import com.aliucord.patcher.Patcher;
import com.aliucord.patcher.PreHook;
import com.aliucord.Utils;
import com.aliucord.utils.GsonUtils;
import com.aliucord.utils.RNSuperProperties;
import com.discord.api.channel.Channel;
import com.discord.models.commands.Application;
import com.discord.models.commands.ApplicationCommand;
import com.discord.models.commands.ApplicationCommandKt;
import com.discord.models.commands.ApplicationCommandLocalSendData;
import com.discord.restapi.RestAPIParams;
import com.discord.stores.BuiltInCommandsProvider;
import com.discord.stores.StoreApplicationCommands;
import com.discord.stores.StoreApplicationCommands$requestApplicationCommands$1;
import com.discord.stores.StoreApplicationCommands$requestApplicationCommandsQuery$1;
import com.discord.stores.StoreApplicationInteractions;
import com.discord.stores.StoreChannelsSelected;
import com.discord.stores.StoreStream;
import com.discord.utilities.error.Error;
import com.discord.utilities.messagesend.MessageResult;
import com.discord.utilities.permissions.PermissionUtils;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

final class Patches {
    private ApplicationIndexCache applicationIndexCache;
    private Logger logger;
    private Method handleGuildApplicationsUpdateMethod;
    private Method handleDiscoverCommandsUpdateMethod;
    private Method handleQueryCommandsUpdateMethod;
    private Field applicationCommandCountField;
    private Field storeApplicationCommandsQueryField;
    private Field errorResponseErrorField;
    private Field skemaErrorSubErrorsField;
    private Field skemaErrorErrorsField;
    private Field skemaErrorItemCodeField;
    private Field skemaErrorItemMessageField;
    private Field storeApplicationCommandsBuiltInCommandsProviderField;
    private DefaultNames defaultNames;

    Patches(Logger logger) throws Throwable {
        this.logger = logger;
        this.applicationIndexCache = new ApplicationIndexCache();
    }

    @SuppressWarnings("unchecked")
    public void loadPatches(Context context) throws Throwable {
        this.handleGuildApplicationsUpdateMethod = StoreApplicationCommands.class.getDeclaredMethod("handleGuildApplicationsUpdate", List.class);
        this.handleGuildApplicationsUpdateMethod.setAccessible(true);
        this.handleDiscoverCommandsUpdateMethod = StoreApplicationCommands.class.getDeclaredMethod("handleDiscoverCommandsUpdate", List.class);
        this.handleDiscoverCommandsUpdateMethod.setAccessible(true);
        this.handleQueryCommandsUpdateMethod = StoreApplicationCommands.class.getDeclaredMethod("handleQueryCommandsUpdate", List.class);
        this.handleQueryCommandsUpdateMethod.setAccessible(true);
        this.applicationCommandCountField = Application.class.getDeclaredField("commandCount");
        this.applicationCommandCountField.setAccessible(true);
        this.storeApplicationCommandsQueryField = StoreApplicationCommands.class.getDeclaredField("query");
        this.storeApplicationCommandsQueryField.setAccessible(true);
        this.errorResponseErrorField = Error.Response.class.getDeclaredField("skemaError");
        this.errorResponseErrorField.setAccessible(true);
        this.skemaErrorSubErrorsField = Error.SkemaError.class.getDeclaredField("subErrors");
        this.skemaErrorSubErrorsField.setAccessible(true);
        this.skemaErrorErrorsField = Error.SkemaError.class.getDeclaredField("errors");
        this.skemaErrorErrorsField.setAccessible(true);
        this.skemaErrorItemCodeField = Error.SkemaErrorItem.class.getDeclaredField("code");
        this.skemaErrorItemCodeField.setAccessible(true);
        this.skemaErrorItemMessageField = Error.SkemaErrorItem.class.getDeclaredField("message");
        this.skemaErrorItemMessageField.setAccessible(true);
        this.storeApplicationCommandsBuiltInCommandsProviderField = StoreApplicationCommands.class.getDeclaredField("builtInCommandsProvider");
        this.storeApplicationCommandsBuiltInCommandsProviderField.setAccessible(true);

        var storeApplicationCommands = StoreStream.getApplicationCommands();
        var storeChannelsSelected = StoreStream.getChannelsSelected();
        var storeUsers = StoreStream.getUsers();
        var storePermissions = StoreStream.getPermissions();
        var storeGuilds = StoreStream.getGuilds();

        // Browsing commands (when just a '/' is typed)
        Patcher.addPatch(
            StoreApplicationCommands$requestApplicationCommands$1.class.getDeclaredMethod("invoke"),
            new PreHook(param -> {
                var this_ = (StoreApplicationCommands$requestApplicationCommands$1) param.thisObject;

                if (this_.$guildId == null) {
                    return;
                }

                try {
                    var applicationIndexSource = Patches.applicationIndexSourceFromContext(this_.$guildId, storeChannelsSelected);
                    this.passCommandData(this_.this$0, applicationIndexSource, RequestSource.BROWSE);
                } catch (RateLimitedException e) {
                    // Already logged once when the limit was hit; don't add more requests meanwhile
                    param.setResult(null);
                    return;
                } catch (Throwable e) {
                    // Let Discord's original request run instead
                    this.logger.error("Failed to load application commands", e);
                    return;
                }

                param.setResult(null);
            })
        );

        // Completing commands
        Patcher.addPatch(
            StoreApplicationCommands$requestApplicationCommandsQuery$1.class.getDeclaredMethod("invoke"),
            new PreHook(param -> {
                var this_ = (StoreApplicationCommands$requestApplicationCommandsQuery$1) param.thisObject;

                if (this_.$guildId == null) {
                    return;
                }

                try {
                    var applicationIndexSource = Patches.applicationIndexSourceFromContext(this_.$guildId, storeChannelsSelected);
                    storeApplicationCommandsQueryField.set(this_.this$0, this_.$query);
                    this.passCommandData(this_.this$0, applicationIndexSource, RequestSource.QUERY);
                } catch (RateLimitedException e) {
                    // Already logged once when the limit was hit; don't add more requests meanwhile
                    param.setResult(null);
                    return;
                } catch (Throwable e) {
                    // Let Discord's original request run instead
                    this.logger.error("Failed to query application commands", e);
                    return;
                }

                param.setResult(null);
            })
        );

        // Command permission check
        Patcher.addPatch(
            ApplicationCommandKt.class.getDeclaredMethod("hasPermission", ApplicationCommand.class, long.class, List.class),
            new InsteadHook(param -> {
                var applicationCommand = (ApplicationCommand) param.args[0];
                var roleIds = (List<Long>) param.args[2];

                if (!(applicationCommand instanceof RemoteApplicationCommand)) {
                    // Allow all builtin commands
                    return true;
                }
                var remoteApplicationCommand = (RemoteApplicationCommand) applicationCommand;

                var channel = storeChannelsSelected.getSelectedChannel();
                var guildId = channel.i();

                if (guildId == 0) {
                    // Allow all commands in DMs
                    return true;
                }

                // This check runs while the command list is rendered, so it must never hit
                // the network. The browse/query hooks fetch the indexes before Discord gets
                // here; a stale entry is still good enough for filtering (the server
                // enforces permissions on execution anyway).
                var applicationId = remoteApplicationCommand.getApplicationId();
                var isUser = this.applicationIndexCache
                    .getAny(new ApplicationIndexSourceUser().getEndpoint())
                    .map(index -> index.applications.containsKey(applicationId))
                    .orElse(false);
                if (isUser) {
                    // Allow all user application commands
                    return true;
                }
                var application = this.applicationIndexCache
                    .getAny(new ApplicationIndexSourceGuild(guildId).getEndpoint())
                    .map(index -> index.applications.get(applicationId))
                    .orElse(null);
                if (application == null) {
                    // Discord requested checking a command from the previous guild - ignore
                    // Some such requests are still processed (if the command exists in both guilds), but it's not an issue as the result doesn't matter for them anyways.
                    return false;
                }
                var user = storeUsers.getMe();
                var memberPermissions = storePermissions.getGuildPermissions()
                    .get(guildId);
                var guild = storeGuilds.getGuild(guildId);

                var applicationPermission = application.permissions_.checkFor(roleIds, channel, guild, memberPermissions, user, true);
                var commandPermission = remoteApplicationCommand.permissions_.checkFor(roleIds, channel, guild, memberPermissions, user, applicationPermission);

                return commandPermission;
            })
        );

        // Command error handling
        Patcher.addPatch(
            StoreApplicationInteractions.class.getDeclaredMethod("handleApplicationCommandResult", MessageResult.class, ApplicationCommandLocalSendData.class, Function0.class, Function1.class),
            new PreHook(param -> {
                var result = (MessageResult) param.args[0];
                var localSendData = (ApplicationCommandLocalSendData) param.args[1];

                if (result instanceof MessageResult.UnknownFailure) {
                    boolean invalidCommandVersion = false;

                    try {
                        var errorResponse = ((MessageResult.UnknownFailure) result)
                            .getError()
                            .getResponse();
                        var error = this.errorResponseErrorField.get(errorResponse);
                        var subErrors = ((Map<String, Error.SkemaError>) skemaErrorSubErrorsField.get(error));
                        var dataErrors = (List<Error.SkemaErrorItem>) skemaErrorErrorsField.get(subErrors.get("data"));

                        for (var dataError: dataErrors) {
                            var errorCode = (String) this.skemaErrorItemCodeField.get(dataError);
                            if (errorCode.equals("INTERACTION_APPLICATION_COMMAND_INVALID_VERSION")) {
                                ApplicationIndexSource applicationIndexSource = null;
                                var guildId = localSendData.component3();
                                if (guildId != null) {
                                    applicationIndexSource = new ApplicationIndexSourceGuild(guildId);
                                } else {
                                    var channelId = localSendData.component2();
                                    applicationIndexSource = new ApplicationIndexSourceDm(channelId);
                                }
                                this.invalidateApplicationIndex(applicationIndexSource);

                                var errorMessage = (String) this.skemaErrorItemMessageField.get(dataError);
                                Utils.showToast(errorMessage);

                                break;
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            })
        );

        // Show localized command names, but send the default ones (see DefaultNames).
        // Every interaction this client sends (commands and autocomplete) is built here.
        try {
            var defaultNames = new DefaultNames(
                com.discord.api.commands.ApplicationCommandData.class,
                com.discord.api.commands.ApplicationCommandValue.class
            );
            Patcher.addPatch(
                RestAPIParams.ApplicationCommand.class.getDeclaredConstructor(
                    long.class, String.class, String.class, String.class,
                    com.discord.api.commands.ApplicationCommandData.class, String.class, String.class
                ),
                new PreHook(param -> {
                    try {
                        defaultNames.applyTo(param.args[4]);
                    } catch (Throwable e) {
                        this.logger.error("Failed to restore default command names", e);
                    }
                })
            );
            this.defaultNames = defaultNames;
        } catch (Throwable e) {
            this.logger.error("Can't rewrite command names when sending; showing default names instead", e);
        }

        GatewayAPI.onEvent("GUILD_APPLICATION_COMMAND_INDEX_UPDATE", ApiGuildApplicationCommandIndexUpdate.class, guildApplicationCommandIndexUpdate -> {
            this.invalidateApplicationIndex(new ApplicationIndexSourceGuild(guildApplicationCommandIndexUpdate.guildId));
            return null;
        });
    }

    private void passCommandData(StoreApplicationCommands storeApplicationCommands, Optional<ApplicationIndexSource> applicationIndexSource, RequestSource requestSource) throws Exception {
        var applicationIndexes = new ArrayList();
        if (applicationIndexSource.isPresent()) {
            applicationIndexes.add(this.requestApplicationIndex(applicationIndexSource.get()));
        }
        try {
            applicationIndexes.add(this.requestApplicationIndex(new ApplicationIndexSourceUser()));
        } catch (IOException e) {
            // User-installed apps unavailable right now; still show the guild/DM commands
            if (applicationIndexes.isEmpty()) throw e;
        }
        var applicationIndex = new ApplicationIndex(applicationIndexes);
        applicationIndex
            .applicationCommands
            .entrySet()
            .removeIf(applicationCommand -> applicationCommand.getValue().type != RemoteApplicationCommand.TYPE_CHAT_INPUT);
        applicationIndex.populateCommandCounts(this.applicationCommandCountField);

        var applications = new ArrayList<Application>(applicationIndex.applications.values());
        Collections.sort(applications, (left, right) -> left.getName().compareTo(right.getName()));
        applications.add(((BuiltInCommandsProvider) this.storeApplicationCommandsBuiltInCommandsProviderField.get(storeApplicationCommands)).getBuiltInApplication());
        this.handleGuildApplicationsUpdateMethod.invoke(storeApplicationCommands, applications);

        switch (requestSource) {
            case BROWSE:
                this.handleDiscoverCommandsUpdateMethod.invoke(storeApplicationCommands, new ArrayList(applicationIndex.applicationCommands.values()));
                break;

            case QUERY:
                this.handleQueryCommandsUpdateMethod.invoke(storeApplicationCommands, new ArrayList(applicationIndex.applicationCommands.values()));
                break;
        }
    }

    /**
     * Returns the application index for a source, fetching it if it isn't cached or is stale.
     * Blocks on network, so only call it off the main thread (the store dispatcher).
     */
    private ApplicationIndex requestApplicationIndex(ApplicationIndexSource source) throws Exception {
        var key = source.getEndpoint();
        var cached = this.applicationIndexCache.getFresh(key);
        if (cached.isPresent()) {
            return cached.get();
        }

        // One request per index at a time; concurrent callers reuse its result
        synchronized (this.applicationIndexCache.lockFor(key)) {
            cached = this.applicationIndexCache.getFresh(key);
            if (cached.isPresent()) {
                return cached.get();
            }

            // Whatever we already have (even if outdated) beats hammering Discord
            var stale = this.applicationIndexCache.getAny(key);
            var blockedForMs = this.applicationIndexCache.blockedForMs(key);
            if (blockedForMs > 0) {
                if (stale.isPresent()) return stale.get();
                throw new RateLimitedException(key, blockedForMs);
            }

            var generation = this.applicationIndexCache.generation(key);
            String json;
            try {
                json = this.fetchText(Http.Request.newDiscordRNRequest(key), key);
            } catch (IOException e) {
                if (!(e instanceof RateLimitedException)) {
                    this.applicationIndexCache.block(key, FAILURE_BACKOFF_MS, false);
                }
                if (stale.isPresent()) return stale.get();
                throw e;
            }

            // `name` is localized and `name_default` is what interactions must contain.
            // Show localized names and let DefaultNames swap them when sending; if that
            // rewrite isn't available, show (and therefore send) the default names.
            var displayJson = json;
            try {
                if (this.defaultNames != null) {
                    this.defaultNames.update(json);
                } else {
                    displayJson = DefaultNames.withDefaultNames(json);
                }
            } catch (Throwable e) {
                this.logger.error("Failed to read default command names", e);
            }
            var applicationIndex = GsonUtils.fromJson(GsonUtils.getGsonRestApi(), displayJson, ApiApplicationIndex.class)
                .toModel();
            this.applicationIndexCache.put(key, applicationIndex, generation, ApplicationIndexCache.MAX_AGE_MS);
            return applicationIndex;
        }
    }

    /** Pause after a failed request (other than a rate limit, which says how long to wait). */
    private static final long FAILURE_BACKOFF_MS = 5 * 1000;

    /**
     * Executes an index request. On 429, honors Retry-After by blocking further requests
     * to that index (or all of them, for a global limit) and throws RateLimitedException.
     */
    private String fetchText(Http.Request request, String key) throws IOException {
        var response = request.execute();
        if (response.statusCode == 429) {
            var retryAfterMs = Patches.retryAfterMs(request);
            var global = "true".equalsIgnoreCase(request.conn.getHeaderField("X-RateLimit-Global"));
            response.close();
            this.applicationIndexCache.block(key, retryAfterMs, global);
            this.logger.warn("Rate limited on " + key + (global ? " (global)" : "") + "; pausing these requests for " + retryAfterMs + " ms");
            throw new RateLimitedException(key, retryAfterMs);
        }
        return response.text();
    }

    /** Retry-After in ms from a 429 response, clamped to [1 s, 10 min]; 5 s if absent. */
    private static long retryAfterMs(Http.Request request) {
        for (var header : new String[] { "Retry-After", "X-RateLimit-Reset-After" }) {
            var value = request.conn.getHeaderField(header);
            if (value == null) continue;
            try {
                var ms = (long) Math.ceil(Double.parseDouble(value.trim()) * 1000);
                return Math.max(1000, Math.min(ms, 10 * 60 * 1000));
            } catch (NumberFormatException ignored) {
            }
        }
        return 5 * 1000;
    }

    private void invalidateApplicationIndex(ApplicationIndexSource source) {
        this.applicationIndexCache.invalidate(source.getEndpoint());
    }

    private static Optional<ApplicationIndexSource> applicationIndexSourceFromContext(long guildId, StoreChannelsSelected storeChannelsSelected) {
        Optional<ApplicationIndexSource> applicationIndexSource = Optional.empty();
        // guildId being 0 means this is a DM or a DM group
        if (guildId != 0) {
            applicationIndexSource = Optional.of(new ApplicationIndexSourceGuild(guildId));
        } else {
            // Only create a DM index source for bots
            var channel = storeChannelsSelected.getSelectedChannel();
            var channelType = channel.D();
            if (channelType == Channel.DM) {
                var user = channel.z().get(0);
                var userIsBot = Optional.ofNullable(user.e())
                    .orElse(false);
                if (userIsBot) {
                    var channelId = channel.k();
                    applicationIndexSource = Optional.of(new ApplicationIndexSourceDm(channelId));
                }
            }
        }

        return applicationIndexSource;
    }
}
