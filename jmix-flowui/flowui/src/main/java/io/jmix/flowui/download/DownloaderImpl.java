/*
 * Copyright 2022 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jmix.flowui.download;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import io.jmix.core.CoreProperties;
import io.jmix.core.FileRef;
import io.jmix.core.FileStorage;
import io.jmix.core.FileStorageException;
import io.jmix.core.FileStorageLocator;
import io.jmix.core.FileTypesHelper;
import io.jmix.core.Messages;
import io.jmix.flowui.UiProperties;
import io.jmix.flowui.asynctask.UiAsyncTasks;
import io.jmix.flowui.component.filedownloader.JmixFileDownloader;
import io.jmix.flowui.exception.IllegalConcurrentAccessException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import org.springframework.lang.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND;

/**
 * Shows exported data in the web browser or downloads it.
 */
@Component("flowui_Downloader")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DownloaderImpl implements Downloader {

    private static final Logger log = LoggerFactory.getLogger(DownloaderImpl.class);
    protected static final String DEFAULT_CHARSET_SUFFIX = ";charset=UTF-8";

    protected UiProperties uiProperties;
    protected CoreProperties coreProperties;

    protected Messages messages;
    protected UiAsyncTasks uiAsyncTasks;

    protected FileStorageLocator fileStorageLocator;
    protected FileStorage fileStorage;

    protected boolean newWindow;

    // Use flags from app.properties for show/download files
    protected boolean useViewList;

    // Predicate for open/download files check
    protected Predicate<String> viewFilePredicate = this::defaultViewFilePredicate;

    /**
     * Constructor with newWindow=false
     */
    public DownloaderImpl() {
        this.newWindow = false;
        this.useViewList = true;
    }

    /**
     * @param newWindow if true, show data in the same browser window;
     *                  if false, open new browser window
     */
    public DownloaderImpl(boolean newWindow) {
        this.newWindow = newWindow;
        this.useViewList = false;
    }

    @Autowired
    public void setUiProperties(UiProperties uiProperties) {
        this.uiProperties = uiProperties;
    }

    @Autowired
    public void setCoreProperties(CoreProperties coreProperties) {
        this.coreProperties = coreProperties;
    }

    @Autowired
    public void setMessages(Messages messages) {
        this.messages = messages;
    }

    @Autowired
    public void setFileStorageLocator(FileStorageLocator fileStorageLocator) {
        this.fileStorageLocator = fileStorageLocator;
    }

    @Autowired
    public void setUiAsyncTasks(UiAsyncTasks uiAsyncTasks) {
        this.uiAsyncTasks = uiAsyncTasks;
    }

    @Override
    public void setFileStorage(FileStorage fileStorage) {
        this.fileStorage = fileStorage;

        log.warn("The passed value is ignored. Actual file storage is obtained from " + FileRef.class.getSimpleName());
    }

    protected boolean defaultViewFilePredicate(String fileExtension) {
        if (StringUtils.isEmpty(fileExtension)) {
            return false;
        }

        return uiProperties.getViewFileExtensions().contains(StringUtils.lowerCase(fileExtension));
    }

    @Override
    public void setViewFilePredicate(Predicate<String> viewFilePredicate) {
        this.viewFilePredicate = viewFilePredicate;
    }

    @Override
    public boolean isShowNewWindow() {
        return newWindow;
    }

    @Override
    public void setShowNewWindow(boolean showNewWindow) {
        this.newWindow = showNewWindow;

        // newWindow is set explicitly
        this.useViewList = false;
    }

    /**
     * Show/Download resource at client side
     *
     * @param dataProvider   DownloadDataProvider
     * @param resourceName   ResourceName for client side
     * @param downloadFormat DownloadFormat
     * @see FileRefDownloadDataProvider
     * @see ByteArrayDownloadDataProvider
     */
    public void download(DownloadDataProvider dataProvider,
                         String resourceName,
                         @Nullable DownloadFormat downloadFormat) {
        checkUIAccess();

        boolean showNewWindow = this.newWindow;

        // Replace all invalid 'resourceName' characters with underscores before downloading.
        // 'resourceName' parameter value will be used in URI (generated when resource is registered)
        // in a way that the name is the last segment of the path
        resourceName = normalize(resourceName);

        if (useViewList) {
            String fileExt;

            if (downloadFormat != null) {
                fileExt = downloadFormat.getFileExt();
            } else {
                fileExt = FilenameUtils.getExtension(resourceName);
            }

            showNewWindow = viewFilePredicate.test(StringUtils.lowerCase(fileExt));
        }

        if (downloadFormat != null) {
            if (StringUtils.isEmpty(FilenameUtils.getExtension(resourceName))) {
                resourceName += "." + downloadFormat.getFileExt();
            }
        }

        JmixFileDownloader fileDownloader = new JmixFileDownloader();

        UI ui = UI.getCurrent();

        ui.add(fileDownloader);

        log.debug("added {} in {}", JmixFileDownloader.class.getSimpleName(), ui);

        fileDownloader.setFileName(resourceName);
        fileDownloader.setCacheMaxAgeSec(uiProperties.getFileDownloaderCacheMaxAgeSec());
        fileDownloader.addDownloadFinishedListener(this::fileDownloaderRemoveHandler);
        fileDownloader.setFileNotFoundExceptionHandler(this::handleFileNotFoundException);

        StreamResource resource = new StreamResource(resourceName, dataProvider::getStream);

        if (downloadFormat != null && StringUtils.isNotEmpty(downloadFormat.getContentType())) {
            resource.setContentType(downloadFormat.getContentType() + DEFAULT_CHARSET_SUFFIX);
        } else {
            resource.setContentType(FileTypesHelper.getMIMEType(resourceName) + DEFAULT_CHARSET_SUFFIX);
        }

        if (showNewWindow && isBrowserSupportsPopups() || isIPhone()) {
            fileDownloader.viewDocument(resource);
        } else {
            fileDownloader.downloadFile(resource);
        }
    }

    /**
     * Show/Download resource at client side
     *
     * @param dataProvider DownloadDataProvider
     * @param resourceName ResourceName for client side
     * @see FileRefDownloadDataProvider
     * @see ByteArrayDownloadDataProvider
     */
    @Override
    public void download(DownloadDataProvider dataProvider, String resourceName) {
        String extension = FilenameUtils.getExtension(resourceName);
        DownloadFormat format = DownloadFormat.getByExtension(extension);
        download(dataProvider, resourceName, format);
    }

    @Override
    public void download(FileRef fileReference) {
        DownloadFormat format = DownloadFormat.getByExtension(
                FilenameUtils.getExtension(fileReference.getFileName())
        );
        download(fileReference, format);
    }

    @Override
    public void download(FileRef fileReference, @Nullable DownloadFormat format) {
        FileStorage fileReferenceStorage = fileStorageLocator.getByName(fileReference.getStorageName());
        String fileName = fileReference.getFileName();
        download(new FileRefDownloadDataProvider(fileReference, fileReferenceStorage), fileName, format);
    }

    @Override
    public void download(byte[] data, String resourceName) {
        ByteArrayDownloadDataProvider dataProvider = new ByteArrayDownloadDataProvider(data,
                uiProperties.getSaveExportedByteArrayDataThresholdBytes(),
                coreProperties.getTempDir());
        download(dataProvider, resourceName);
    }

    @Override
    public void download(byte[] data, String resourceName, @Nullable DownloadFormat format) {
        ByteArrayDownloadDataProvider dataProvider = new ByteArrayDownloadDataProvider(data,
                uiProperties.getSaveExportedByteArrayDataThresholdBytes(),
                coreProperties.getTempDir());
        download(dataProvider, resourceName, format);
    }

    protected void checkUIAccess() {
        VaadinSession vaadinSession = VaadinSession.getCurrent();

        if (vaadinSession == null || !vaadinSession.hasLock()) {
            throw new IllegalConcurrentAccessException();
        }
    }

    protected void fileDownloaderRemoveHandler(JmixFileDownloader.DownloadFinishedEvent event) {
        uiAsyncTasks.runnableConfigurer(() -> {
                    try {
                        // timer
                        TimeUnit.SECONDS.sleep(60);
                    } catch (InterruptedException e) {
                        log.debug("{} exception in background task", e.getClass().getName(), e);
                    }
                })
                .withResultHandler(event.getSource()::removeFromParent)
                .withTimeout(62, TimeUnit.SECONDS)
                .runAsync();
    }

    protected boolean handleFileNotFoundException(JmixFileDownloader.FileNotFoundContext fileNotFoundEvent) {
        Exception exception = fileNotFoundEvent.getException();
        VaadinResponse response = fileNotFoundEvent.getResponse();

        if (!(exception instanceof FileStorageException storageException)) {
            return false;
        }

        if (storageException.getType() == FileStorageException.Type.FILE_NOT_FOUND) {
            try {
                String message = messages.getMessage("fileNotFound.message");
                String formattedMessage = String.format(message, storageException.getFileName());

                writeFileNotFoundException(response, formattedMessage);
                return true;
            } catch (IOException e) {
                log.debug("Can't write file not found exception to the response body for: {}",
                        storageException.getFileName(), e);
                return false;
            }
        } else {
            return false;
        }
    }

    protected void writeFileNotFoundException(VaadinResponse response, String message) throws IOException {
        response.setStatus(SC_NOT_FOUND);
        response.setHeader("Content-Type", "text/html; charset=utf-8");

        String outputStr = "<h1 style=\"font-size:40px;\">404</h1><p style=\"font-size: 25px\">" + message + "</p>";
        byte[] outputBytes = outputStr.getBytes(StandardCharsets.UTF_8);
        response.getOutputStream().write(outputBytes);
        response.getOutputStream().flush();
    }

    protected String normalize(String originString) {
        return originString.replaceAll("[/\\\\]", "_");
    }

    protected boolean isBrowserSupportsPopups() {
        return !VaadinSession.getCurrent().getBrowser().isSafari();
    }

    protected boolean isIPhone() {
        return VaadinSession.getCurrent().getBrowser().isIPhone();
    }
}
