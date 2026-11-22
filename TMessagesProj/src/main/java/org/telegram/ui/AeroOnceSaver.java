package org.telegram.ui;

import android.text.TextUtils;
import android.widget.Toast;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AeroConfig;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.VideoEditedInfo;
import org.telegram.messenger.secretmedia.EncryptedFileInputStream;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Locale;

/**
 * AeroGram 1-martalik (View-Once / TTL) va oddiy media/xabarlarni Saved Messages ga doimiy nusxalovchi yordamchi.
 */
public final class AeroOnceSaver {

    private AeroOnceSaver() {}

    public static void saveToSavedMessages(int currentAccount, BaseFragment fragment, MessageObject msg) {
        if (msg == null || msg.messageOwner == null) {
            return;
        }
        final long selfId = UserConfig.getInstance(currentAccount).getClientUserId();
        if (selfId == 0) {
            return;
        }

        final String senderName = resolveSenderName(currentAccount, msg);
        final boolean isOnce = msg.isSecretMedia() || msg.isVoiceOnce() || msg.isRoundOnce();

        // 1. Oddiy matnli xabar
        if (!msg.isPhoto() && msg.getDocument() == null) {
            CharSequence text = msg.messageText;
            if (TextUtils.isEmpty(text) && msg.messageOwner != null) {
                text = msg.messageOwner.message;
            }
            if (TextUtils.isEmpty(text)) {
                showFeedback(fragment, AeroConfig.tr("Saqlash uchun matn yoki media topilmadi", "Сақлаш учун матн ёки медиа топилмади"));
                return;
            }
            String header = "🔒 " + AeroConfig.tr("Saqlangan xabar", "Сақланган хабар")
                + (TextUtils.isEmpty(senderName) ? "" : " (" + senderName + ")") + ":\n\n";
            SendMessagesHelper.getInstance(currentAccount).sendMessage(
                SendMessagesHelper.SendMessageParams.of(header + text.toString(), selfId)
            );
            showFeedback(fragment, AeroConfig.tr("Saved Messages ga saqlandi", "Saved Messages га сақланди"));
            return;
        }

        // 2. Mahalliy keshda tayyor faylni qidirish
        ResolvedFile local = resolveLocalMediaFile(currentAccount, msg);
        if (local != null) {
            dispatchCopyAndSend(currentAccount, fragment, msg, local, selfId, senderName, isOnce);
            return;
        }

        // 3. Hali ochilmagan / yuklanmagan media -> fon yuklashni boshlash va tugashi bilan Saved Messages ga tashlash
        startDownloadAndSave(currentAccount, fragment, msg, selfId, senderName, isOnce);
    }

    private static final class ResolvedFile {
        final File sourceFile;
        final File keyFile;
        final boolean encrypted;

        ResolvedFile(File sourceFile, File keyFile, boolean encrypted) {
            this.sourceFile = sourceFile;
            this.keyFile = keyFile;
            this.encrypted = encrypted;
        }
    }

    private static ResolvedFile resolveLocalMediaFile(int currentAccount, MessageObject msg) {
        if (!TextUtils.isEmpty(msg.messageOwner.attachPath)) {
            File f = new File(msg.messageOwner.attachPath);
            ResolvedFile hit = checkPlainOrEncrypted(f);
            if (hit != null) {
                return hit;
            }
        }

        FileLoader loader = FileLoader.getInstance(currentAccount);
        File msgPath = loader.getPathToMessage(msg.messageOwner);
        ResolvedFile hit = checkPlainOrEncrypted(msgPath);
        if (hit != null) {
            return hit;
        }

        if (msg.isPhoto() && msg.photoThumbs != null && !msg.photoThumbs.isEmpty()) {
            TLRPC.PhotoSize big = FileLoader.getClosestPhotoSizeWithSize(msg.photoThumbs, AndroidUtilities.getPhotoSize(true), false, null, true);
            if (big == null) {
                big = FileLoader.getClosestPhotoSizeWithSize(msg.photoThumbs, AndroidUtilities.getPhotoSize());
            }
            if (big != null) {
                File p1 = loader.getPathToAttach(big, null, true);
                hit = checkPlainOrEncrypted(p1);
                if (hit != null) {
                    return hit;
                }
                File p2 = loader.getPathToAttach(big, null, false);
                hit = checkPlainOrEncrypted(p2);
                if (hit != null) {
                    return hit;
                }
            }
        }

        TLRPC.Document doc = msg.getDocument();
        if (doc != null) {
            File d1 = loader.getPathToAttach(doc, null, true);
            hit = checkPlainOrEncrypted(d1);
            if (hit != null) {
                return hit;
            }
            File d2 = loader.getPathToAttach(doc, null, false);
            hit = checkPlainOrEncrypted(d2);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    private static ResolvedFile checkPlainOrEncrypted(File plainCandidate) {
        if (plainCandidate == null || TextUtils.isEmpty(plainCandidate.getPath())) {
            return null;
        }
        if (plainCandidate.exists() && plainCandidate.length() > 0) {
            return new ResolvedFile(plainCandidate, null, false);
        }
        File encFile = new File(plainCandidate.getAbsolutePath() + ".enc");
        if (encFile.exists() && encFile.length() > 0) {
            File keyFile = new File(FileLoader.getInternalCacheDir(), plainCandidate.getName() + ".enc.key");
            if (keyFile.exists()) {
                return new ResolvedFile(encFile, keyFile, true);
            }
        }
        return null;
    }

    private static void startDownloadAndSave(int currentAccount, BaseFragment fragment, MessageObject msg, long selfId, String senderName, boolean isOnce) {
        final FileLoader loader = FileLoader.getInstance(currentAccount);
        final TLObject targetObj;
        final ImageLocation photoLoc;
        if (msg.isPhoto() && msg.photoThumbs != null && !msg.photoThumbs.isEmpty()) {
            TLRPC.PhotoSize big = FileLoader.getClosestPhotoSizeWithSize(msg.photoThumbs, AndroidUtilities.getPhotoSize(true), false, null, true);
            if (big == null) {
                big = FileLoader.getClosestPhotoSizeWithSize(msg.photoThumbs, AndroidUtilities.getPhotoSize());
            }
            targetObj = big;
            photoLoc = big != null ? ImageLocation.getForObject(big, msg.photoThumbsObject) : null;
        } else {
            targetObj = msg.getDocument();
            photoLoc = null;
        }

        if (targetObj == null) {
            showFeedback(fragment, AeroConfig.tr("Media topilmadi", "Медиа топилмади"));
            return;
        }

        final String expectedName = FileLoader.getAttachFileName(targetObj);
        if (TextUtils.isEmpty(expectedName)) {
            showFeedback(fragment, AeroConfig.tr("Fayl nomi aniqlanmadi", "Файл номи аниқланмади"));
            return;
        }
        final int dotIdx = expectedName.lastIndexOf('.');
        final String baseStem = dotIdx > 0 ? expectedName.substring(0, dotIdx) : expectedName;
        final NotificationCenter nc = NotificationCenter.getInstance(currentAccount);

        final NotificationCenter.NotificationCenterDelegate[] holder = new NotificationCenter.NotificationCenterDelegate[1];
        final boolean[] finished = new boolean[1];

        final Runnable timeoutRunnable = () -> {
            if (!finished[0]) {
                finished[0] = true;
                nc.removeObserver(holder[0], NotificationCenter.fileLoaded);
                nc.removeObserver(holder[0], NotificationCenter.fileLoadFailed);
                showFeedback(fragment, AeroConfig.tr("Yuklash vaqti tugadi, qayta urinib ko‘ring", "Юклаш вақти тугади, қайта уриниб кўринг"));
            }
        };

        holder[0] = (id, account, args) -> {
            if (finished[0] || args == null || args.length == 0 || !(args[0] instanceof String)) {
                return;
            }
            String loadedName = (String) args[0];
            if (!TextUtils.equals(loadedName, expectedName) && !loadedName.startsWith(baseStem)) {
                return;
            }
            finished[0] = true;
            AndroidUtilities.cancelRunOnUIThread(timeoutRunnable);
            nc.removeObserver(holder[0], NotificationCenter.fileLoaded);
            nc.removeObserver(holder[0], NotificationCenter.fileLoadFailed);

            if (id == NotificationCenter.fileLoadFailed) {
                showFeedback(fragment, AeroConfig.tr("Mediani yuklab bo‘lmadi", "Медиани юклаб бўлмади"));
                return;
            }

            ResolvedFile resolved = resolveLocalMediaFile(currentAccount, msg);
            if (resolved == null && args.length > 1 && args[1] instanceof File) {
                resolved = checkPlainOrEncrypted((File) args[1]);
            }
            if (resolved != null) {
                dispatchCopyAndSend(currentAccount, fragment, msg, resolved, selfId, senderName, isOnce);
            } else {
                showFeedback(fragment, AeroConfig.tr("Saqlash uchun fayl topilmadi", "Сақлаш учун файл топилмади"));
            }
        };

        nc.addObserver(holder[0], NotificationCenter.fileLoaded);
        nc.addObserver(holder[0], NotificationCenter.fileLoadFailed);
        AndroidUtilities.runOnUIThread(timeoutRunnable, 35000L);

        showFeedback(fragment, AeroConfig.tr("Yuklab olinib Saved Messages ga saqlanmoqda...", "Юклаб олиниб Saved Messages га сақланмоқда..."));

        if (photoLoc != null) {
            loader.loadFile(photoLoc, msg, "jpg", FileLoader.PRIORITY_HIGH, 1);
        } else if (targetObj instanceof TLRPC.Document) {
            loader.loadFile((TLRPC.Document) targetObj, msg, FileLoader.PRIORITY_HIGH, 1);
        }
    }

    private static void dispatchCopyAndSend(int currentAccount, BaseFragment fragment, MessageObject msg, ResolvedFile resolved, long selfId, String senderName, boolean isOnce) {
        Utilities.globalQueue.postRunnable(() -> {
            File cacheDir = FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE);
            if (cacheDir == null) {
                cacheDir = FileLoader.getInternalCacheDir();
            }
            String ext = guessExtension(msg, resolved.sourceFile);
            File destFile = new File(cacheDir, "aero_saved_" + Math.abs(msg.getId()) + "_" + System.currentTimeMillis() + ext);

            boolean ok;
            if (resolved.encrypted && resolved.keyFile != null) {
                ok = decryptEncFile(resolved.sourceFile, resolved.keyFile, destFile);
            } else {
                ok = AndroidUtilities.copyFileSafe(resolved.sourceFile, destFile);
            }

            if (!ok || !destFile.exists() || destFile.length() <= 0) {
                AndroidUtilities.runOnUIThread(() ->
                    showFeedback(fragment, AeroConfig.tr("Faylni nusxalashda xatolik yuz berdi", "Файлни нусхалашда хатолик юз берди"))
                );
                return;
            }

            final String destPath = destFile.getAbsolutePath();
            final String caption = buildCaption(msg, senderName, isOnce);

            AndroidUtilities.runOnUIThread(() -> sendPlainFileToSaved(currentAccount, fragment, msg, destFile, destPath, caption, selfId));
        });
    }

    private static boolean decryptEncFile(File encFile, File keyFile, File destFile) {
        try (EncryptedFileInputStream is = new EncryptedFileInputStream(encFile, keyFile);
             FileOutputStream out = new FileOutputStream(destFile)) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf, 0, buf.length)) > 0) {
                out.write(buf, 0, n);
            }
            out.flush();
            return true;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }

    private static void sendPlainFileToSaved(int currentAccount, BaseFragment fragment, MessageObject msg, File destFile, String destPath, String caption, long selfId) {
        try {
            AccountInstance accountInstance = AccountInstance.getInstance(currentAccount);
            if (msg.isPhoto()) {
                SendMessagesHelper.prepareSendingPhoto(
                    accountInstance, destPath, null, selfId,
                    null, null, null, caption, null, null, null,
                    0, null, true, 0, 0, null
                );
            } else if (msg.isRoundVideo() || msg.isRoundOnce()) {
                VideoEditedInfo roundInfo = SendMessagesHelper.createAeroRoundVideoSettings(destPath);
                SendMessagesHelper.prepareSendingVideo(
                    accountInstance, destPath, roundInfo, null, null, selfId,
                    null, null, null, null, null, 0, null, true, 0, 0,
                    false, false, null, null, 0, 0
                );
            } else if (msg.isVideo()) {
                SendMessagesHelper.prepareSendingVideo(
                    accountInstance, destPath, null, null, null, selfId,
                    null, null, null, null, null, 0, null, true, 0, 0,
                    false, false, caption, null, 0, 0
                );
            } else if (msg.isVoice() || msg.isVoiceOnce()) {
                TLRPC.TL_document voiceDoc = buildFreshVoiceDocument(currentAccount, msg.getDocument(), destFile, selfId);
                SendMessagesHelper.SendMessageParams params = SendMessagesHelper.SendMessageParams.of(
                    voiceDoc, null, destPath, selfId, null, null,
                    caption, null, null, null, true, 0, 0, 0, null, null, false
                );
                SendMessagesHelper.getInstance(currentAccount).sendMessage(params);
            } else {
                ArrayList<String> paths = new ArrayList<>();
                paths.add(destPath);
                TLRPC.Document origDoc = msg.getDocument();
                String mime = origDoc != null && !TextUtils.isEmpty(origDoc.mime_type) ? origDoc.mime_type : null;
                SendMessagesHelper.prepareSendingDocuments(
                    accountInstance, paths, paths, null, caption, mime, selfId,
                    null, null, null, null, null, true, 0, null, null, 0, false, 0
                );
            }
            showFeedback(fragment, AeroConfig.tr("🔒 Saved Messages ga saqlandi!", "🔒 Saved Messages га сақланди!"));
        } catch (Exception e) {
            FileLog.e(e);
            showFeedback(fragment, AeroConfig.tr("Saqlashda xatolik yuz berdi", "Сақлашда хатолик юз берди"));
        }
    }

    private static TLRPC.TL_document buildFreshVoiceDocument(int currentAccount, TLRPC.Document origDoc, File destFile, long selfId) {
        TLRPC.TL_document doc = new TLRPC.TL_document();
        doc.file_reference = new byte[0];
        doc.dc_id = Integer.MIN_VALUE;
        doc.id = SharedConfig.getLastLocalId();
        doc.user_id = selfId;
        doc.date = ConnectionsManager.getInstance(currentAccount).getCurrentTime();
        doc.mime_type = "audio/ogg";
        doc.size = destFile.length();
        SharedConfig.saveConfig();

        TLRPC.TL_documentAttributeAudio attr = new TLRPC.TL_documentAttributeAudio();
        attr.voice = true;
        if (origDoc != null && origDoc.attributes != null) {
            for (int i = 0; i < origDoc.attributes.size(); i++) {
                TLRPC.DocumentAttribute a = origDoc.attributes.get(i);
                if (a instanceof TLRPC.TL_documentAttributeAudio) {
                    TLRPC.TL_documentAttributeAudio srcAudio = (TLRPC.TL_documentAttributeAudio) a;
                    attr.duration = srcAudio.duration;
                    attr.waveform = srcAudio.waveform;
                    if (attr.waveform != null) {
                        attr.flags |= 4;
                    }
                    break;
                }
            }
        }
        doc.attributes.add(attr);
        return doc;
    }

    private static String guessExtension(MessageObject msg, File sourceFile) {
        if (msg.isPhoto()) {
            return ".jpg";
        }
        if (msg.isVoice() || msg.isVoiceOnce()) {
            return ".ogg";
        }
        if (msg.isVideo() || msg.isRoundVideo() || msg.isRoundOnce()) {
            return ".mp4";
        }
        TLRPC.Document doc = msg.getDocument();
        if (doc != null) {
            String docName = FileLoader.getDocumentFileName(doc);
            if (!TextUtils.isEmpty(docName)) {
                int dot = docName.lastIndexOf('.');
                if (dot >= 0) {
                    return docName.substring(dot);
                }
            }
            if (!TextUtils.isEmpty(doc.mime_type) && doc.mime_type.toLowerCase(Locale.US).contains("webp")) {
                return ".webp";
            }
        }
        if (sourceFile != null) {
            String name = sourceFile.getName();
            if (name.endsWith(".enc")) {
                name = name.substring(0, name.length() - 4);
            }
            int dot = name.lastIndexOf('.');
            if (dot >= 0) {
                return name.substring(dot);
            }
        }
        return "";
    }

    private static String resolveSenderName(int currentAccount, MessageObject msg) {
        try {
            long senderId = msg.getSenderId();
            if (senderId > 0) {
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(senderId);
                if (user != null) {
                    return UserObject.getUserName(user);
                }
            } else if (senderId < 0) {
                TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-senderId);
                if (chat != null && !TextUtils.isEmpty(chat.title)) {
                    return chat.title;
                }
            }
            long did = msg.getDialogId();
            if (DialogObject.isUserDialog(did)) {
                TLRPC.User u = MessagesController.getInstance(currentAccount).getUser(did);
                if (u != null) {
                    return UserObject.getUserName(u);
                }
            }
        } catch (Exception ignore) {}
        return "";
    }

    private static String buildCaption(MessageObject msg, String senderName, boolean isOnce) {
        String tag = isOnce
            ? AeroConfig.tr("🔒 Saqlangan (1-martalik)", "🔒 Сақланган (1-марталик)")
            : AeroConfig.tr("🔒 Saqlangan media", "🔒 Сақланган медиа");
        if (!TextUtils.isEmpty(senderName)) {
            tag += " • " + senderName;
        }
        if (!TextUtils.isEmpty(msg.caption)) {
            return tag + "\n" + msg.caption;
        }
        return tag;
    }

    private static void showFeedback(BaseFragment fragment, String message) {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                if (fragment != null && fragment.getParentActivity() != null && fragment.getFragmentView() != null) {
                    BulletinFactory.of(fragment).createSimpleBulletin(R.raw.saved_messages, message).show();
                    return;
                }
            } catch (Exception ignore) {}
            if (ApplicationLoader.applicationContext != null) {
                Toast.makeText(ApplicationLoader.applicationContext, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
