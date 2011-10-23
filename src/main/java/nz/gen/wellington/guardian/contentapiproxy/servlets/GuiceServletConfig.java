package nz.gen.wellington.guardian.contentapiproxy.servlets;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;

public class GuiceServletConfig extends GuiceServletContextListener {
	
	private static Logger log = Logger.getLogger(GuiceServletConfig.class);

	protected static final String PROPERTIES_FILE = "guardian-content-proxy.properties";

	@Override
	protected Injector getInjector() {
		return Guice.createInjector(new ServletModule() {

			@Override
			protected void configureServlets() {
				try {
					bindProperties();

					serve("/about").with(AboutProxyServlet.class);
					serve("/saved").with(SavedProxyServlet.class);
					serve("/search").with(SearchProxyServlet.class);
					serve("/sections").with(SectionProxyServlet.class);
					serve("/tags").with(TagsProxyServlet.class);

				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			private void bindProperties() throws FileNotFoundException, IOException {
				URL propertiesResource = this.getClass().getClassLoader().getResource(PROPERTIES_FILE);
				log.info("Properties file url is: " + propertiesResource.toExternalForm());
				Properties properties = new Properties();
				properties.load(new FileInputStream(propertiesResource.getFile()));
				Names.bindProperties(binder(), properties);
			}
			
		});
	}

}
