package io.appform.statesman.server.resources;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.appform.statesman.model.request.CreateProvider;
import io.appform.statesman.server.dao.providers.ProviderCommands;
import io.appform.statesman.server.utils.ProviderUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Produces(MediaType.APPLICATION_JSON)
@Path("/v1/provider")
@Slf4j
@Api("Provider related APIs")
@Singleton
public class ProviderResource {

    private final ProviderCommands providerCommands;

    @Inject
    public ProviderResource(ProviderCommands providerCommands){
        this.providerCommands = providerCommands;
    }


    @POST
    @Path("/create")
    @ApiOperation("Creates provider")
    public void save(@Valid CreateProvider request){
          providerCommands.save(ProviderUtils.toDao(request));
    }


}
