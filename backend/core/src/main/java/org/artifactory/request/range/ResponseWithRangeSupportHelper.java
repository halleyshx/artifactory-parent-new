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

package org.artifactory.request.range;

import org.artifactory.request.range.stream.MultiRangeInputStream;
import org.artifactory.request.range.stream.SingleRangeInputStream;
import org.artifactory.request.range.stream.SingleRangeSkipInputStream;
import org.jfrog.storage.binstore.ifc.SkippableInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static javax.servlet.http.HttpServletResponse.SC_PARTIAL_CONTENT;
import static org.artifactory.common.ConstantValues.httpRangeSupport;
import static org.artifactory.request.range.IfRangeSelector.constructIfRange;
import static org.artifactory.request.range.Range.constructRange;

/**
 * The class creates context that contains the data needed to decorate the httpResponse in order to support HTTP ranges
 *
 * @author Gidi Shabat
 */
public class ResponseWithRangeSupportHelper {
    static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String MULTIPART_BYTERANGES_BOUNDRY = "BCD64322345343217845286A";
    private static final String MULTIPART_BYTERANGES_HEADER = "multipart/byteranges; boundary=" + MULTIPART_BYTERANGES_BOUNDRY;
    static final String MULTIPART_BYTERANGES_BOUNDRY_SEP = "--" + MULTIPART_BYTERANGES_BOUNDRY;
    public static final String MULTIPART_BYTERANGES_BOUNDRY_END = MULTIPART_BYTERANGES_BOUNDRY_SEP + "--";
    private static final Logger log = LoggerFactory.getLogger(ResponseWithRangeSupportHelper.class);

    public static RangeAwareContext createRangeAwareContext(InputStream in, long length, String rangesString,
            String ifRange, String mimeType, long lastModified, String sha1) throws IOException {
        RangeAwareContext context = new RangeAwareContext();
        // resolve the If-Range behaviour from the If-Range header
        IfRangeSelector ifRangeSelector = constructIfRange(ifRange, lastModified, sha1);
        // Resolve the ranges from the ranges header
        List<Range> ranges = constructRange(rangesString, mimeType, length, context, ifRangeSelector);
        // Do not continue if error occurred
        if (context.getStatus() > 0) {
            return context;
        }
        // Fill the result context according to the ranges.
        if (!httpRangeSupport.getBoolean() || ranges.size() == 0) {
            handleSimpleResponse(in, length, mimeType, context);
        } else if (ranges.size() == 1) {
            handleSingleRangeResponse(in, ranges, mimeType, context);
        } else {
            handleMultiRangeResponse(in, ranges, context);
        }
        return context;
    }

    private static void handleSimpleResponse(InputStream in, long length, String mimeType, RangeAwareContext context)
            throws IOException {
        log.debug("Preparing response for simple response (None range response)");
        // Update headers, content type, content length and logs
        context.setContentLength(length);
        context.setContentType(mimeType);
        context.setInputStream(in);
    }

    private static void handleSingleRangeResponse(InputStream in, List<Range> ranges, String mimeType,
            RangeAwareContext context) throws IOException {
        log.debug("Preparing response for single range response");
        // Resolve range
        Range range = ranges.get(0);
        String totalLength;
        long end;
        if (range.getEntityLength() >= 0) {
            totalLength = Long.toString(range.getEntityLength());
            end = Math.min(range.getEnd(), range.getEntityLength() - 1L);
        } else {
            totalLength = "*";
            end = range.getEnd();
        }
        String contentRange = "bytes " + range.getStart() + "-" + end + "/" + totalLength;
        context.setStatus(SC_PARTIAL_CONTENT);
        context.setContentType(mimeType);
        context.setContentRange(contentRange);
        context.setContentLength(end - range.getStart() + 1L);
        if(isSkippable(in)){
            context.setInputStream(new SingleRangeSkipInputStream(range, in));
        }else {
            context.setInputStream(new SingleRangeInputStream(range, in));
        }
    }

    private static boolean isSkippable(InputStream inputStream) {
        return !(inputStream instanceof SkippableInputStream) || ((SkippableInputStream) inputStream).isSkippable();
    }

    private static void handleMultiRangeResponse(InputStream in, List<Range> ranges, RangeAwareContext context)
            throws IOException {
        log.debug("Preparing response for multi range response");
        // calculate response content length
        long length = MULTIPART_BYTERANGES_BOUNDRY_END.length() + 2;
        for (Range r : ranges) {
            length += r.getLength();
        }
        context.setStatus(SC_PARTIAL_CONTENT);
        context.setContentType(MULTIPART_BYTERANGES_HEADER);
        context.setContentLength(length);
        context.setInputStream(new MultiRangeInputStream(ranges, in));
    }
}
