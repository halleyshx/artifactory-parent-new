package org.artifactory.repo.http

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import spock.lang.Specification

class GuavaCacheConnectionManagersHolderSpec extends Specification {

    def "explicit add remove"() {
        given:
        ConnectionManagersHolder holder = new GuaveCacheConnectionManagersHolder()

        when:
        holder.put("a", new PoolingHttpClientConnectionManager())
        holder.put("b", new PoolingHttpClientConnectionManager())
        holder.put("c", new PoolingHttpClientConnectionManager())

        then:
        holder.size() == 3

        when:
        holder.remove("a")
        holder.remove("b")

        then:
        holder.size() == 1

    }

    def "test gc cleans unreferenced keys"() {
        given:
        List keys = [new String("a"), new String("b"), new String("c")]
        ConnectionManagersHolder holder = new GuaveCacheConnectionManagersHolder()


        when:
        keys.each { String key ->
            holder.put(key, new PoolingHttpClientConnectionManager())
        }

        then:
        holder.size() == 3

        when: "setting the keys to null, so it will get collected by gc. Enforce gc"
        keys = null
        System.gc()
        System.finalize()
        ((GuaveCacheConnectionManagersHolder) holder).connections.cleanUp()

        then: "holder vals are getting cleaned"
        holder.values().size() == 0

    }
}