/*
 *
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2016 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.artifactory.rest.response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.io.output.NullWriter;
import org.artifactory.addon.rest.AuthorizationRestException;
import org.artifactory.common.ConstantValues;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.request.ArtifactoryResponseBase;
import org.artifactory.rest.common.exception.RestException;
import org.artifactory.util.HttpUtils;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Date;
import java.util.function.Consumer;

/**
 * @author Shay Yaakov
 */
public class JerseyArtifactoryResponse extends ArtifactoryResponseBase {
    private static final Logger log = LoggerFactory.getLogger(JerseyArtifactoryResponse.class);

    private Response.ResponseBuilder response;
    // reference ensuring we will consume the stream.
    private Closeable mustConsume;
    final private Exception createdTraceException;

    public JerseyArtifactoryResponse() {
        response = Response.ok();
        // For log.trace debug leaks.
        if (log.isTraceEnabled()) {
            createdTraceException = new RuntimeException("Created stacktrace");
        } else {
            createdTraceException = null;
        }
    }

    @Override
    public void sendStream(final InputStream is) throws IOException {
        mustConsume = is;

        // the method set in entity is being used by jersey threads.
        // which might not be called at all - if it was HEAD request.
        response.status(getStatus()).entity((StreamingOutput) out -> {
            try {
                IOUtils.copy(is, out);
            } finally {
                // using ref to actual object prevent object finalize till done.
                JerseyArtifactoryResponse mustHeldRefPreventPrematureFinalise = JerseyArtifactoryResponse.this;
                mustHeldRefPreventPrematureFinalise.close();
            }
        });
    }

    @Override
    protected void finalize() throws Throwable {
        checkWarnUnconsumedStream();
        close();
        super.finalize();
    }

    private void close() {
        IOUtils.closeQuietly(mustConsume);
        // prevent rerelease of stream. we do it only once.
        mustConsume = null;
    }

    private void checkWarnUnconsumedStream() {
        if (mustConsume != null) {
            String msg = getClass().getName() +
                    " finalize caught unconsumed stream by Jersey. That can indicate a bug and starvation of connections";
            if (log.isTraceEnabled()) {
                log.error(msg, createdTraceException);
            } else {
                log.error(msg);
            }
            if (log.isTraceEnabled()) {
                log.warn("finalize detected on closed stream, we have mustHeldRefPreventPrematureFinalise in sendStream,"+
                        " Jersey might have been skipping the entity method."+
                        " Use with Head request while isHead was not overridden.");
            }
        }
    }


    /**
     * Delegates writing to this response's output stream to the {@link Consumer} given as {@param delegate}
     */
    public void sendStreamWithDelegation(Consumer<OutputStream> delegate) {
        response.status(getStatus()).entity((StreamingOutput) delegate::accept);
    }

    @Override
    protected void sendErrorInternal(int code, String reason) throws IOException {
        throw new RestException(code, reason);
    }

    @Override
    public void setLastModified(long lastModified) {
        response.lastModified(new Date(lastModified));
    }

    @Override
    public void setContentLength(long length) {
        super.setContentLength(length);
        setHeader("Content-Length", String.valueOf(length));
    }

    @Override
    public void setEtag(String etag) {
        if (etag != null) {
            response.header("ETag", etag);
        } else {
            log.debug("Could not register a null etag with the response.");
        }
    }

    @Override
    public void setSha1(String sha1) {
        if (sha1 != null) {
            response.header(ArtifactoryRequest.CHECKSUM_SHA1, sha1);
        } else {
            log.debug("Could not register a null sha1 tag with the response.");
        }
    }

    @Override
    public void setSha2(String sha2) {
        if (sha2 != null) {
            response.header(ArtifactoryRequest.CHECKSUM_SHA256, sha2);
        } else {
            log.debug("Could not register a null sha256 tag with the response.");
        }
    }

    @Override
    public void setMd5(String md5) {
        if (md5 != null) {
            response.header(ArtifactoryRequest.CHECKSUM_MD5, md5);
        } else {
            log.debug("Could not register a null md5 tag with the response.");
        }
    }

    @Override
    public void setRangeSupport(String rangeSupport) {
        if (rangeSupport != null) {
            response.header(ArtifactoryRequest.ACCEPT_RANGES, rangeSupport);
        } else {
            log.debug("Could not register a null range support tag with the response.");
        }
    }

    @Override
    public void setContentType(String contentType) {
        response.type(contentType);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return new NullOutputStream();
    }

    @Override
    public void setContentDispositionAttachment(String filename) {
        if (ConstantValues.responseDisableContentDispositionFilename.getBoolean() || StringUtils.isBlank(filename)) {
            response.header("Content-Disposition", "attachment");
        } else {
            response.header("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        }
    }

    @Override
    public void setFilename(String filename) {
        if (StringUtils.isNotBlank(filename)) {
            response.header(ArtifactoryRequest.FILE_NAME, HttpUtils.encodeQuery(filename));
        } else {
            log.debug("Could not register a null filename with the response.");
        }
    }

    @Override
    public Writer getWriter() throws IOException {
        return new NullWriter();
    }

    @Override
    public void setStatus(int status) {
        super.setStatus(status);
        response.status(status);
    }

    @Override
    public void setHeader(String header, String value) {
        response.header(header, value);
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void flush() {

    }

    @Override
    public void sendAuthorizationRequired(String message, String realm) throws IOException {
        throw new AuthorizationRestException(message);
    }

    @Override
    public void close(Closeable closeable) {
        // NOP, we use Jersey's StreamingOutput so cannot close here
        // done by finalizer
        if ((this.mustConsume != null) && (this.mustConsume != closeable)) {
            // we probably inherit from the stream
            sanityCheckCloseableMatch(closeable);
        }
        else {
            this.mustConsume = closeable;
        }
    }

    private void sanityCheckCloseableMatch(Closeable closeable) {
        if ((this.mustConsume != null) && (this.mustConsume != closeable)) {
            if (log.isTraceEnabled()) {
                log.warn("Would close only the underlying stream. closable which differs the what given in sendStream. Created by: ",
                        createdTraceException);
            }
        }
    }

    public Response build() {
        return response.build();
    }
}
