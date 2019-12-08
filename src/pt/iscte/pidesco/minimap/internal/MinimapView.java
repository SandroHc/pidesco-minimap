/*
 * Copyright (c) 2019.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors:
 *  - Sandro Marques <https://sandrohc.net>
 */

package pt.iscte.pidesco.minimap.internal;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import pt.iscte.pidesco.extensibility.PidescoView;
import pt.iscte.pidesco.javaeditor.service.JavaEditorListener;
import pt.iscte.pidesco.javaeditor.service.JavaEditorServices;
import pt.iscte.pidesco.minimap.Activator;
import pt.iscte.pidesco.minimap.internal.listeners.ButtonClicked;
import pt.iscte.pidesco.minimap.internal.parser.MinimapFile;
import pt.iscte.pidesco.minimap.internal.parser.MinimapLine;

public class MinimapView implements PidescoView {

	private static final Logger LOGGER = Logger.getLogger(MinimapView.class);
	public static final String VIEW_ID = "pt.iscte.pidesco.minimap.minimap";

	public static final MinimapLine EMPTY_LINE = new MinimapLine(1, "No file opened.");
	public static final List<MinimapLine> EMPTY_LINE_LIST = Collections.singletonList(EMPTY_LINE);

	/** the current instance for the plugin view */
	private static MinimapView instance;

	private final JavaEditorServices javaEditorServices;

	/* SWT Components */
	private Composite root;
	private ScrolledComposite scroll;


	public MinimapView() {
		instance = this;

		BundleContext context = Activator.getContext();
		ServiceReference<JavaEditorServices> serviceReference = context.getServiceReference(JavaEditorServices.class);
		javaEditorServices = context.getService(serviceReference);

		loadFilters();
	}

	public static MinimapView getInstance() {
		return instance;
	}
	
	private void loadFilters() {
//		LOGGER.info("Filters loaded");
//		IExtensionRegistry reg = Platform.getExtensionRegistry();
//		for(IExtension ext : reg.getExtensionPoint(EXT_POINT_FILTER).getExtensions()) {
//			
//			ProjectBrowserFilter f = null;
//			try {
//				f = (ProjectBrowserFilter) ext.getConfigurationElements()[0].createExecutableExtension("class");
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			if(f != null)
//				filtersMap.put(ext.getUniqueIdentifier(), new Filter(f));
//		}
	}

	@Override
	public void createContents(Composite viewArea, Map<String, Image> images) {
		this.root = viewArea;

		JavaEditorListener listener = new JavaEditorListener() {
			@Override
			public void fileOpened(File file) {
				LOGGER.debug("File opened: " + file);

				// TODO: do this in a worker thread
				Collection<MinimapLine> lines = new MinimapFile(file).parse(javaEditorServices);

				createScrollComponent(lines);


//				StyledText text = new StyledText(scroll, SWT.BORDER);
//				text.setText("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");
//
//				FontData data = text.getFont().getFontData()[0];
//				Font font1 = new Font(scroll.getDisplay(), data.getName(), data.getHeight(), data.getStyle());
//
//				StyleRange style1 = new StyleRange();
//				style1.start = 0;
//				style1.length = 10;
//				style1.fontStyle = SWT.BOLD;
//				style1.font = font1;
//				style1.background = Colors.PURPLE;
//				text.setStyleRange(style1);
			}

			@Override
			public void fileClosed(File file) {
				LOGGER.debug("File closed: " + file);
			}
			
			@Override
			public void fileSaved(File file) {
				LOGGER.debug("File saved: " + file);
				// TODO reload/reparse file
			}
		};

		javaEditorServices.addListener(listener);

		File openedFile = javaEditorServices.getOpenedFile();
		if (openedFile != null) {
			listener.fileOpened(openedFile);
		} else {
			createScrollComponent(EMPTY_LINE_LIST);
		}
	}
	
	private void createScrollComponent(Collection<MinimapLine> lines) {
		// https://www.codeaffine.com/2016/03/01/swt-scrolledcomposite

		if (this.scroll == null || this.scroll.isDisposed()) {
			// Create a new ScrolledComposite
			LOGGER.debug("Creating new ScrolledComposite");

			ScrolledComposite scroll = new ScrolledComposite(this.root, SWT.V_SCROLL);
			scroll.setBackground(this.root.getDisplay().getSystemColor(SWT.COLOR_YELLOW));
			scroll.setExpandHorizontal(true);
			this.scroll = scroll;
		} else if (this.scroll.getContent() != null && !this.scroll.getContent().isDisposed()) {
			// Remove the ScrolledComposite children
			LOGGER.debug("Removing ScrolledComposite content");

			this.scroll.getContent().dispose();
		}

		// https://www.eclipse.org/articles/Article-Understanding-Layouts/Understanding-Layouts.htm
		RowLayout layout = new RowLayout(SWT.VERTICAL);
		layout.fill = true;
//		layout.spacing = 0;
//		layout.marginTop = 0;

		Group group = new Group(scroll, SWT.SHADOW_NONE);
		group.setLayout(layout);

		for (MinimapLine line : lines) {
			Label label = MinimapLine.createLabel(line, group);
			label.addMouseListener(
					new ButtonClicked(() -> {
						LOGGER.debug("Label clicked");
//						javaEditorServices.selectText();
					})
			);
		}

		group.pack();
		scroll.setContent(group);
	}

}