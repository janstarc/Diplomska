package com.diplomska.hapi;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Condition;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import ca.uhn.fhir.model.dstu2.valueset.BundleTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.HTTPVerbEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static com.diplomska.constants.address.HapiCryptoChangeKey;
import static com.diplomska.constants.address.HapiCryptoObservation;
import static com.diplomska.constants.address.HapiRESTfulServer;
import static com.diplomska.crypto.cryptoDB.updateKeyAlias;

@WebServlet(urlPatterns = {"/hapi.do/ChangeKey"})
public class hapiChangeKey extends HttpServlet {

    HttpClient httpClient = HttpClientBuilder.create().build();
    URIBuilder uri;

    // Get requesti - iskanje pacientov
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Find patient and set new key
        String _id = request.getParameter("_id");
        String keyAlias = request.getParameter("keyAlias");

        try {
            // Send request to crypto --> Encrypt _id
            uri = new URIBuilder(HapiCryptoChangeKey);
            //uri.setParameter("encrypt", "true");
            uri.setParameter("_id", _id);
            uri.setParameter("keyAlias", keyAlias);
            HttpGet requestToCrypto = new HttpGet(String.valueOf(uri));
            HttpResponse changedKeyResources = httpClient.execute(requestToCrypto);
            int statusCode = changedKeyResources.getStatusLine().getStatusCode();

            if(statusCode != 200){
                System.out.println("Status code: " + statusCode);
                response.sendError(statusCode);
                return;
            }


            //System.out.println("Response code: " + changedKeyResources.co);
            /**
             *  TODO
             *  -Nastavi error code, ce enkripcija ni uspesna/kljuc ne obstaja
             *  -Nalozi resource na HAPI Server
             *  -Ce je response od HAPIja OK, popravi keyAlias vnos v bazi za userja
             *
             *  TODO 2
             *  -Shandlaj dodajanje novega userja - s katerim kljucem se kriptira
             *
             *
             */
            String changeKeyResponse = EntityUtils.toString(changedKeyResources.getEntity());

            System.out.println("HAPI Response: \n" + changeKeyResponse);

            FhirContext ctx = FhirContext.forDstu2();
            IGenericClient client = ctx.newRestfulGenericClient(HapiRESTfulServer);

            Bundle encryptedResources = (Bundle) ctx.newJsonParser().setPrettyPrint(true).parseResource(changeKeyResponse);

            // Create a client and post the transaction to the server
            Bundle resp = client.transaction().withBundle(encryptedResources).execute();
            updateKeyAlias(_id, keyAlias);

            // Log the response
            System.out.println(ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(resp));

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
