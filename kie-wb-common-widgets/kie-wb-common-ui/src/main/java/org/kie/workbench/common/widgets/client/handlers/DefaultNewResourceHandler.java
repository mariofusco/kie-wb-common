package org.kie.workbench.common.widgets.client.handlers;

import java.util.LinkedList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import com.google.gwt.core.client.Callback;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.IsWidget;
import org.jboss.errai.bus.client.api.RemoteCallback;
import org.jboss.errai.ioc.client.api.Caller;
import org.kie.commons.data.Pair;
import org.kie.workbench.common.services.project.service.ProjectService;
import org.kie.workbench.common.services.shared.context.KieWorkbenchContext;
import org.kie.workbench.common.services.shared.context.Package;
import org.kie.workbench.common.widgets.client.resources.i18n.CommonConstants;
import org.kie.workbench.common.widgets.client.widget.BusyIndicatorView;
import org.uberfire.backend.vfs.Path;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.client.workbench.type.ClientResourceType;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.mvp.impl.PathPlaceRequest;
import org.uberfire.workbench.events.NotificationEvent;

/**
 * Handler for the creation of new Items that require a Name and Path
 */
public abstract class DefaultNewResourceHandler implements NewResourceHandler {

    protected final List<Pair<String, ? extends IsWidget>> extensions = new LinkedList<Pair<String, ? extends IsWidget>>();

    protected final PathLabel pathLabel = new PathLabel();

    @Inject
    protected KieWorkbenchContext context;

    @Inject
    protected Caller<ProjectService> projectService;

    @Inject
    private PlaceManager placeManager;

    @Inject
    private Event<NotificationEvent> notificationEvent;

    @Inject
    private BusyIndicatorView busyIndicatorView;

    @PostConstruct
    private void setupExtensions() {
        this.extensions.add( Pair.newPair( CommonConstants.INSTANCE.ItemPathSubheading(),
                                           pathLabel ) );
    }

    @Override
    public List<Pair<String, ? extends IsWidget>> getExtensions() {
        final Package activePackage = context.getActivePackage();
        this.pathLabel.setPath( ( activePackage == null ? null : activePackage.getPackageMainResourcesPath() ) );
        return this.extensions;
    }

    @Override
    public boolean validate() {
        boolean isValid = true;
        if ( pathLabel.getPath() == null ) {
            Window.alert( CommonConstants.INSTANCE.MissingPath() );
            isValid = false;
        }
        return isValid;
    }

    @Override
    public void acceptPath( final Path path,
                            final Callback<Boolean, Void> callback ) {
        if ( path == null ) {
            callback.onSuccess( false );
        } else {
            final Package pkg = context.getActivePackage();
            boolean accept = ( pkg == null ? false : pkg.getPackageMainResourcesPath() != null );
            callback.onSuccess( accept );
        }
    }

    protected String buildFileName( final ClientResourceType resourceType,
                                    final String baseFileName ) {
        final String extension = "." + resourceType.getSuffix();
        if ( baseFileName.endsWith( extension ) ) {
            return resourceType.getPrefix() + baseFileName;
        }
        return resourceType.getPrefix() + baseFileName + extension;
    }

    protected void notifySuccess() {
        notificationEvent.fire( new NotificationEvent( CommonConstants.INSTANCE.ItemCreatedSuccessfully() ) );
    }

    protected RemoteCallback<Path> getSuccessCallback( final NewResourcePresenter presenter ) {
        return getSuccessCallback( presenter, null );
    }

    protected RemoteCallback<Path> getSuccessCallback( final NewResourcePresenter presenter,
                                                       final Command postSaveCommand ) {
        return new RemoteCallback<Path>() {

            @Override
            public void callback( final Path path ) {
                busyIndicatorView.hideBusyIndicator();
                presenter.complete();
                notifySuccess();
                executePostSaveCommand();
                final PlaceRequest place = new PathPlaceRequest( path );
                placeManager.goTo( place );
            }

            private void executePostSaveCommand() {
                if ( postSaveCommand != null ) {
                    postSaveCommand.execute();
                }
            }

        };
    }
}