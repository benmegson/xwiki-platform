/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.cache.oscache.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.EntryEvictionConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.container.Container;

import com.opensymphony.oscache.base.algorithm.LRUCache;
import com.opensymphony.oscache.plugins.diskpersistence.DiskPersistenceListener;
import com.opensymphony.oscache.plugins.diskpersistence.HashDiskPersistenceListener;

/**
 * Convert XWiki cache configuration into OSCache configuration.
 * 
 * @version $Id: $
 */
public class OSCacheCacheConfiguration
{
    /**
     * The logging tool.
     */
    private static final Log LOG = LogFactory.getLog(OSCacheCacheConfiguration.class);

    /**
     * The folder containing OSCache properties files.
     */
    private static final String PROPS_PATH = "cache/oscache/";

    /**
     * The extension of OSCache properties files.
     */
    private static final String PROPS_EXT = ".properties";

    /**
     * The name of the cache.path property in OSCache configuration.
     */
    private static final String CONFOC_CACHE_PATH = "cache.path";

    /**
     * The name of the cache.path property in XWiki configuration.
     */
    private static final String CONFX_CACHE_PATH = CONFOC_CACHE_PATH;

    /**
     * The default configuration identifier used to load cache configuration file.
     */
    protected String defaultPropsId = "default";

    /**
     * The XWiki cache API configuration.
     */
    private CacheConfiguration configuration;

    /**
     * The OSCAche configuration.
     */
    private Properties oscacheConfiguration;

    /**
     * The container used to access configuration files.
     */
    private Container container;

    /**
     * The maximum duration a cache entry can exists without any modification.
     */
    private int timeToLive = -1;

    /**
     * The name of the cache.
     */
    private String name;

    /**
     * @param container the container used to access configuration files.
     * @param configuration the XWiki cache API configuration.
     * @param defaultPropsId the default configuration identifier used to load cache configuration file.
     */
    public OSCacheCacheConfiguration(Container container, CacheConfiguration configuration, String defaultPropsId)
    {
        this.container = container;
        this.configuration = configuration;
        this.defaultPropsId = defaultPropsId;

        load();
    }

    /**
     * @return the XWiki cache API configuration.
     */
    public CacheConfiguration getCacheConfiguration()
    {
        return this.configuration;
    }

    /**
     * @return the OSCAche configuration.
     */
    public Properties getOSCacheProperties()
    {
        return this.oscacheConfiguration;
    }

    /**
     * @return the maximum duration a cache entry can exists without any modification.
     */
    public int getTimeToLive()
    {
        return this.timeToLive;
    }

    /**
     * @return the name of the cache.
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * Load OSCache properties file.
     */
    private void load()
    {
        this.oscacheConfiguration = getDefaultConfig();

        EntryEvictionConfiguration ec = null;

        try {
            this.oscacheConfiguration = getConfig(this.configuration.getConfigurationId());
        } catch (Exception e) {
            for (Map.Entry<String, Object> entry : this.configuration.entrySet()) {
                if (entry.getKey() == EntryEvictionConfiguration.CONFIGURATIONID) {
                    ec = (EntryEvictionConfiguration) entry.getValue();

                    if (ec.getAlgorithm() == EntryEvictionConfiguration.Algorithm.LRU) {
                        this.oscacheConfiguration.setProperty("cache.algorithm", LRUCache.class.getName());
                        if (ec.containsKey(LRUEvictionConfiguration.MAXENTRIES_ID)) {
                            this.oscacheConfiguration.setProperty("cache.capacity", ec.get(
                                LRUEvictionConfiguration.MAXENTRIES_ID).toString());
                        }
                    }
                }
            }
        }

        if (ec != null && ec.getTimeToLive() > 0) {
            timeToLive = ec.getTimeToLive();
        }

        this.name = this.configuration.getConfigurationId();

        if (this.name != null) {
            this.oscacheConfiguration.setProperty("cache.key", this.name);
        }

        completePersistanceListenerConfiguration();
    }

    /**
     * Add missing configuration needed by some OSCache PersistanceListener implementations.
     */
    private void completePersistanceListenerConfiguration()
    {
        String persistanceListener = this.oscacheConfiguration.getProperty("cache.persistence.class");
        if (persistanceListener != null
            && this.oscacheConfiguration.getProperty(CONFOC_CACHE_PATH) == null
            && (HashDiskPersistenceListener.class.getName().equals(persistanceListener) || DiskPersistenceListener.class
                .getName().equals(persistanceListener))) {

            String path = (String) this.configuration.get(CONFX_CACHE_PATH);

            if (path == null) {
                path = System.getProperty("java.io.tmpdir") + File.separator + "xwiki";
                if (this.configuration.getConfigurationId() == null) {
                    path += File.separator + this.configuration.getConfigurationId() + File.separator;
                }

                File tempDir = new File(path);
                if (!tempDir.exists()) {
                    tempDir.mkdirs();
                }
            }

            this.oscacheConfiguration.setProperty(CONFX_CACHE_PATH, path);
        }
    }

    /**
     * @return the default OSCache configuration.
     */
    private Properties getDefaultConfig()
    {
        Properties defaultConfig = new Properties();

        try {
            loadConfig(defaultConfig, this.defaultPropsId);
        } catch (PropertiesLoadingCacheException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not load default cache properties", e);
            }
        }

        return defaultConfig;
    }

    /**
     * Load OSCache property file based on configuration identifier.
     * 
     * @param propertiesId the configuration identifier.
     * @return the OSCAche configuration.
     * @throws PropertiesLoadingCacheException error when loading OSCache configuration.
     */
    private Properties getConfig(String propertiesId) throws PropertiesLoadingCacheException
    {
        if (LOG.isInfoEnabled()) {
            LOG.info("Loading cache properties: " + propertiesId);
        }

        Properties props = getDefaultConfig();

        loadConfig(props, propertiesId);

        return props;
    }

    /**
     * Load OSCache property file based on configuration identifier.
     * 
     * @param props properties object to fill with configuration.
     * @param propertiesId the configuration identifier.
     * @throws PropertiesLoadingCacheException error when loading OSCache configuration.
     */
    private void loadConfig(Properties props, String propertiesId) throws PropertiesLoadingCacheException
    {
        String propertiesFilename = propertiesId + PROPS_EXT;

        File file = new File(PROPS_PATH + propertiesFilename);

        InputStream is = null;

        try {
            if (file.exists()) {
                is = new FileInputStream(file);
            } else {
                is =
                    this.container.getApplicationContext().getResourceAsStream(
                        "/WEB-INF/" + PROPS_PATH + propertiesFilename);
            }

            if (is == null) {
                throw new PropertiesLoadingCacheException("Can't find any configuration file for" + propertiesId);
            }

            props.load(is);

            if (LOG.isInfoEnabled()) {
                LOG.info("Properties loaded: " + propertiesFilename);
            }
        } catch (Exception e) {
            throw new PropertiesLoadingCacheException("Error when trying to load configuration file for ["
                + propertiesId + "]");
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Failed t close properties file", e);
                    }
                }
            }
        }
    }
}
