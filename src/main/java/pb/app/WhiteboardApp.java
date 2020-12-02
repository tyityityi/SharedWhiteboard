package pb.app;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import pb.WhiteboardServer;
import pb.managers.ClientManager;
import pb.managers.IOThread;
import pb.managers.PeerManager;
import pb.managers.ServerManager;
import pb.managers.endpoint.Endpoint;


/**
 * Initial code obtained from:
 * https://www.ssaurel.com/blog/learn-how-to-make-a-swing-painting-and-drawing-application/
 */
public class WhiteboardApp {
	private static Logger log = Logger.getLogger(WhiteboardApp.class.getName());
	
	/**
	 * Emitted to another peer to subscribe to updates for the given board. Argument
	 * must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String listenBoard = "BOARD_LISTEN";

	/**
	 * Emitted to another peer to unsubscribe to updates for the given board.
	 * Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String unlistenBoard = "BOARD_UNLISTEN";

	/**
	 * Emitted to another peer to get the entire board data for a given board.
	 * Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String getBoardData = "GET_BOARD_DATA";

	/**
	 * Emitted to another peer to give the entire board data for a given board.
	 * Argument must have format "host:port:boardid%version%PATHS".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardData = "BOARD_DATA";

	/**
	 * Emitted to another peer to add a path to a board managed by that peer.
	 * Argument must have format "host:port:boardid%version%PATH". The numeric value
	 * of version must be equal to the version of the board without the PATH added,
	 * i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardPathUpdate = "BOARD_PATH_UPDATE";

	/**
	 * Emitted to another peer to indicate a new path has been accepted. Argument
	 * must have format "host:port:boardid%version%PATH". The numeric value of
	 * version must be equal to the version of the board without the PATH added,
	 * i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardPathAccepted = "BOARD_PATH_ACCEPTED";

	/**
	 * Emitted to another peer to remove the last path on a board managed by that
	 * peer. Argument must have format "host:port:boardid%version%". The numeric
	 * value of version must be equal to the version of the board without the undo
	 * applied, i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardUndoUpdate = "BOARD_UNDO_UPDATE";

	/**
	 * Emitted to another peer to indicate an undo has been accepted. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the undo applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardUndoAccepted = "BOARD_UNDO_ACCEPTED";

	/**
	 * Emitted to another peer to clear a board managed by that peer. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the clear applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardClearUpdate = "BOARD_CLEAR_UPDATE";

	/**
	 * Emitted to another peer to indicate an clear has been accepted. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the clear applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardClearAccepted = "BOARD_CLEAR_ACCEPTED";

	/**
	 * Emitted to another peer to indicate a board no longer exists and should be
	 * deleted. Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardDeleted = "BOARD_DELETED";

	/**
	 * Emitted to another peer to indicate an error has occurred.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardError = "BOARD_ERROR";
	
	/**
	 * White board map from board name to board object 
	 */
	Map<String,Whiteboard> whiteboards;
	
	/**
	 * The currently selected white board
	 */
	Whiteboard selectedBoard = null;
	
	/**
	 * The peer:port string of the peer. This is synonomous with IP:port, host:port,
	 * etc. where it may appear in comments.
	 */
	String peerport="standalone"; // a default value for the non-distributed version
	
	/*
	 * GUI objects, you probably don't need to modify these things... you don't
	 * need to modify these things... don't modify these things [LOTR reference?].
	 */
	
	JButton clearBtn, blackBtn, redBtn, createBoardBtn, deleteBoardBtn, undoBtn;
	JCheckBox sharedCheckbox ;
	DrawArea drawArea;
	JComboBox<String> boardComboBox;
	boolean modifyingComboBox=false;
	boolean modifyingCheckBox=false;
	
	
	private PeerManager peerManager;
	private ClientManager indexManager;
	private Endpoint indexClient;
	private volatile boolean blocker=true;
	/**
	 * Initialize the white board app.
	 */
	public WhiteboardApp(int peerPort,String whiteboardServerHost, 
			int whiteboardServerPort) {
		whiteboards=new HashMap<>();
		this.peerport="127.0.0.1:"+peerPort;
		peerManager = new PeerManager(peerPort);
		peerManager.on(PeerManager.peerStarted, (args)->{
			Endpoint server=(Endpoint)args[0];
			ServerManager serverManager=(ServerManager)args[1];
			server.on(getBoardData, (args2)->{
				String boardName=(String)args2[0];
				String boardData=whiteboards.get(boardName).toString();
				server.emit(this.boardData, boardData);
			}).on(listenBoard, (args2)->{
				String boardName=(String)args2[0];
				Whiteboard board=whiteboards.get(boardName);
				board.subscribers.add(server);
			}).on(unlistenBoard, (args2)->{
				String boardName=(String)args2[0];
				Whiteboard board=whiteboards.get(boardName);
				board.subscribers.remove(server);
				serverManager.endpointClosed(server);
			}).on(boardPathUpdate, (args2)->{
				String boardData=(String)args2[0];
				String boardName=getBoardName(boardData);
				long boardVersion=getBoardVersion(boardData);
				String data=getBoardData(boardData);
				Whiteboard board=whiteboards.get(boardName);
				if(boardVersion==board.getVersion()) {
					board.whiteboardFromString(boardName, data);
					board.setVersion(boardVersion++);
					if(selectedBoard.getName().equals(boardName)){
						drawSelectedWhiteboard();
					}
					for(Endpoint subscriber:board.subscribers) {
						subscriber.emit(boardPathAccepted, board.toString());
					}
				}
			}).on(boardClearUpdate, (args2)->{
				String boardData=(String)args2[0];
				String boardName=getBoardName(boardData);
				long boardVersion=getBoardVersion(boardData);
				String data=getBoardData(boardData);
				Whiteboard board=whiteboards.get(boardName);
				if(boardVersion==board.getVersion()) {
					board.whiteboardFromString(boardName, data);
					board.setVersion(boardVersion++);
					if(selectedBoard.getName().equals(boardName)){
						drawSelectedWhiteboard();
					}
					for(Endpoint subscriber:board.subscribers) {
						subscriber.emit(boardClearAccepted, board.toString());
					}
				}
			}).on(boardUndoUpdate, (args2)->{
				String boardData=(String)args2[0];
				String boardName=getBoardName(boardData);
				long boardVersion=getBoardVersion(boardData);
				Whiteboard board=whiteboards.get(boardName);
				if(boardVersion==board.getVersion()) {
					board.undo(boardVersion);
					board.setVersion(boardVersion++);
					if(selectedBoard.getName().equals(boardName)){
						drawSelectedWhiteboard();
					}
					for(Endpoint subscriber:board.subscribers) {
						subscriber.emit(boardUndoAccepted, board.getNameAndVersion()+"%");
					}
				}
			});
		}).on(PeerManager.peerStopped, (args)->{
			Endpoint server=(Endpoint)args[0];
			log.info("session stop from: "+server.getOtherEndpointId());
		}).on(PeerManager.peerError, (args)->{
			Endpoint server=(Endpoint)args[0];
			log.info("session error from: "+server.getOtherEndpointId());
		}).on(PeerManager.peerServerManager, (args)->{
			try {
				ClientManager clientManager = peerManager.connect(whiteboardServerPort, whiteboardServerHost);
				clientManager.on(PeerManager.peerStarted, (args2)->{
					indexClient = (Endpoint)args2[0];
					indexClient.on(WhiteboardServer.sharingBoard,(args3)->{
						String board=(String)args3[0];
						if(!whiteboards.containsKey(board)) {
							getBoardFromPeer(board);
						}else if((getIP(board)+":"+getPort(board)).equals(this.peerport)) {
							Whiteboard sharedb=whiteboards.get(board);
							sharedb.setShared(true);
						}
					}).on(WhiteboardServer.unsharingBoard,(args3)->{
						String board=(String)args3[0];
						if(whiteboards.containsKey(board)) {
							if(!(getIP(board)+":"+getPort(board)).equals(this.peerport)) {
								Whiteboard localboard=whiteboards.get(board);
								localboard.manageEndpoint.localEmit(boardDeleted, board);
							}else {
								Whiteboard sharedb=whiteboards.get(board);
								if(sharedb!=null)
									sharedb.setShared(false);
							}
						}
					});
				}).on(PeerManager.peerStopped, (args2)->{
					Endpoint server=(Endpoint)args2[0];
					ClientManager manager=(ClientManager)args2[1];
					manager.endpointClosed(server);
					manager.shutdown();
					log.info("session stop from: "+server.getOtherEndpointId());
				}).on(PeerManager.peerError, (args2)->{
					Endpoint server=(Endpoint)args2[0];
					ClientManager manager=(ClientManager)args2[1];
					manager.endpointClosed(server);
					manager.shutdown();
					log.info("session error from: "+server.getOtherEndpointId());
				});
				indexManager=clientManager;
				clientManager.start();
			} catch (UnknownHostException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
        show(peerport);
		peerManager.start();

    }
	
	/******
	 * 
	 * Utility methods to extract fields from argument strings.
	 * 
	 ******/
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return peer:port:boardid
	 */
	public static String getBoardName(String data) {
		String[] parts=data.split("%",2);
		return parts[0];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return boardid%version%PATHS
	 */
	public static String getBoardIdAndData(String data) {
		String[] parts=data.split(":");
		return parts[2];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return version%PATHS
	 */
	public static String getBoardData(String data) {
		String[] parts=data.split("%",2);
		return parts[1];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return version
	 */
	public static long getBoardVersion(String data) {
		String[] parts=data.split("%",3);
		return Long.parseLong(parts[1]);
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return PATHS
	 */
	public static String getBoardPaths(String data) {
		String[] parts=data.split("%",3);
		return parts[2];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return peer
	 */
	public static String getIP(String data) {
		String[] parts=data.split(":");
		return parts[0];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return port
	 */
	public static int getPort(String data) {
		String[] parts=data.split(":");
		return Integer.parseInt(parts[1]);
	}
	
	/******
	 * 
	 * Methods called from events.
	 * 
	 ******/
	
	// From whiteboard server
	
	
	// From whiteboard peer
	public void getBoardFromPeer(String boardName) {
		int port=getPort(boardName);
		String host=getIP(boardName);
		try {
			ClientManager clientManager = peerManager.connect(port,host);
			clientManager.on(PeerManager.peerStarted, (args)->{
				Endpoint client = (Endpoint)args[0];
				ClientManager manager=(ClientManager)args[1];
				client.on(boardData, (args2)->{
					String boardData=(String)args2[0];
					String paths=getBoardPaths(boardData);
					long version=getBoardVersion(boardData);
					Whiteboard board=new Whiteboard(boardName,paths,version,true);
					board.manageEndpoint=client;
					addBoard(board, false);
					client.emit(listenBoard, boardName);
				}).on(boardPathAccepted, (args2)->{
					String boardData=(String)args2[0];
					String data=getBoardData(boardData);
					String name=getBoardName(boardData);
					Whiteboard board=whiteboards.get(name);
					board.whiteboardFromString(name, data);
					if(selectedBoard.getName().equals(name)) {
						drawSelectedWhiteboard();
					}
				}).on(boardClearAccepted, (args2)->{
					String boardData=(String)args2[0];
					String data=getBoardData(boardData);
					String name=getBoardName(boardData);
					Whiteboard board=whiteboards.get(name);
					board.whiteboardFromString(name, data);
					if(selectedBoard.getName().equals(name)) {
						drawSelectedWhiteboard();
					}
				}).on(boardUndoAccepted, (args2)->{
					String boardData=(String)args2[0];
					long version=getBoardVersion(boardData);
					String name=getBoardName(boardData);
					Whiteboard board=whiteboards.get(name);
					board.undo(board.getVersion());
					board.setVersion(version);
					if(selectedBoard.getName().equals(name)) {
						drawSelectedWhiteboard();
					}
				}).on(boardDeleted, (args2)->{
					String name=(String)args2[0];
					whiteboards.remove(name);
					updateComboBox(null);
					manager.endpointClosed(client);
					manager.shutdown();
				});
				client.emit(getBoardData, boardName);
			}).on(ClientManager.sessionStopped, (args)->{
	        	Endpoint client = (Endpoint)args[0];
	        	log.info("session stop from: "+client.getOtherEndpointId());
	        }).on(ClientManager.sessionError, (args)->{
	        	Endpoint client = (Endpoint)args[0];
	        	log.info("session error from: "+client.getOtherEndpointId());
	        });
			clientManager.start();
		} catch (UnknownHostException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/******
	 * 
	 * Methods to manipulate data locally. Distributed systems related code has been
	 * cut from these methods.
	 * 
	 ******/
	
	/**
	 * Wait for the peer manager to finish all threads.
	 */
	public void waitToFinish() {
		while(blocker) {}
		log.info("peerManager end!");
		blocker=true;

    }
	
	/**
	 * Add a board to the list that the user can select from. If select is
	 * true then also select this board.
	 * @param whiteboard
	 * @param select
	 */
	public void addBoard(Whiteboard whiteboard,boolean select) {
		synchronized(whiteboards) {
			whiteboards.put(whiteboard.getName(), whiteboard);
		}
		updateComboBox(select?whiteboard.getName():null);
	}
	
	/**
	 * Delete a board from the list.
	 * @param boardname must have the form peer:port:boardid
	 */
	public void deleteBoard(String boardname) {
		synchronized(whiteboards) {
			Whiteboard whiteboard = whiteboards.get(boardname);
			if(whiteboard!=null) {
				if(!selectedBoard.isRemote()) {				
					if(selectedBoard.isShared()) {
						indexClient.emit(WhiteboardServer.unshareBoard, boardname);
//						for(Endpoint subscriber:selectedBoard.subscribers) {
//							subscriber.emit(boardDeleted, selectedBoard.getName());
//						}
					}
				}else {
					Endpoint manager=selectedBoard.manageEndpoint;
					manager.emit(unlistenBoard, selectedBoard.getName());
				}
				whiteboards.remove(boardname);
			}
		}
		updateComboBox(null);
	}
	
	/**
	 * Create a new local board with name peer:port:boardid.
	 * The boardid includes the time stamp that the board was created at.
	 */
	public void createBoard() {
		String name = peerport+":board"+Instant.now().toEpochMilli();
		Whiteboard whiteboard = new Whiteboard(name,false);
		addBoard(whiteboard,true);
	}
	
	/**
	 * Add a path to the selected board. The path has already
	 * been drawn on the draw area; so if it can't be accepted then
	 * the board needs to be redrawn without it.
	 * @param currentPath
	 */
	public void pathCreatedLocally(WhiteboardPath currentPath) {
		if(selectedBoard!=null) {
			if(!selectedBoard.isRemote()) {
				if(!selectedBoard.addPath(currentPath,selectedBoard.getVersion())) {
					drawSelectedWhiteboard();
				}else {
					if(selectedBoard.isShared()) {
						for(Endpoint subscriber:selectedBoard.subscribers) {
							subscriber.emit(boardPathAccepted, selectedBoard.toString());
						}
					}
				};
			}else {
				Endpoint manager=selectedBoard.manageEndpoint;
				String board=selectedBoard.toString()+"%"+currentPath.toString();
				//selectedBoard.addPath(currentPath,selectedBoard.getVersion());
				manager.emit(boardPathUpdate, board);
				//drawSelectedWhiteboard();
			}
		} else {
			log.severe("path created without a selected board: "+currentPath);
		}
	}
	
	/**
	 * Clear the selected whiteboard.
	 */
	public void clearedLocally() {
		if(selectedBoard!=null) {
			if(!selectedBoard.isRemote()) {
				if(selectedBoard.clear(selectedBoard.getVersion())) {
					if(selectedBoard.isShared()) {
						for(Endpoint subscriber:selectedBoard.subscribers) {
							subscriber.emit(boardClearAccepted, selectedBoard.toString());
						}
					}	
				};
				drawSelectedWhiteboard();
			}else {
				Endpoint manager=selectedBoard.manageEndpoint;
				String board=selectedBoard.getNameAndVersion()+"%";
				manager.emit(boardClearUpdate, board);
			}
			
//			if(!selectedBoard.clear(selectedBoard.getVersion())) {
//				// some other peer modified the board in between
//				drawSelectedWhiteboard();
//			} else {
//				// was accepted locally, so do remote stuff if needed
//				
//				drawSelectedWhiteboard();
//			}
		} else {
			log.severe("cleared without a selected board");
		}
	}
	
	/**
	 * Undo the last path of the selected whiteboard.
	 */
	public void undoLocally() {
		if(selectedBoard!=null) {
			if(!selectedBoard.isRemote()) {
				if(selectedBoard.undo(selectedBoard.getVersion())) {
					if(selectedBoard.isShared()) {
						String board=selectedBoard.getNameAndVersion()+"%";
						for(Endpoint subscriber:selectedBoard.subscribers) {
							subscriber.emit(boardUndoAccepted,board);
						}
					}	
				};
				drawSelectedWhiteboard();
			}else {
				Endpoint manager=selectedBoard.manageEndpoint;
				String board=selectedBoard.getNameAndVersion()+"%";
				manager.emit(boardUndoUpdate, board);
			}
//			if(!selectedBoard.undo(selectedBoard.getVersion())) {
//				// some other peer modified the board in between
//				drawSelectedWhiteboard();
//			} else {
//				
//				drawSelectedWhiteboard();
//			}
		} else {
			log.severe("undo without a selected board");
		}
	}
	
	/**
	 * The variable selectedBoard has been set.
	 */
	public void selectedABoard() {
		drawSelectedWhiteboard();
		log.info("selected board: "+selectedBoard.getName());
	}
	
	/**
	 * Set the share status on the selected board.
	 */
	public void setShare(boolean share) {
		if(selectedBoard!=null) {
			try {
				if(share)
					indexClient.emit(WhiteboardServer.shareBoard, selectedBoard.getName());
				else
					indexClient.emit(WhiteboardServer.unshareBoard, selectedBoard.getName());
			} catch (Exception e) {
				e.printStackTrace();
			}
        } else {
        	log.severe("there is no selected board");
        }
	}
	
	/**
	 * Called by the gui when the user closes the app.
	 */
	public void guiShutdown() {
		// do some final cleanup
		HashSet<Whiteboard> existingBoards= new HashSet<>(whiteboards.values());
		existingBoards.forEach((board)->{
			deleteBoard(board.getName());
		});
//    	whiteboards.values().forEach((whiteboard)->{});
		try {
			log.info("guiShutdown:blocker want to close");
			peerManager.shutdown();
		}catch(Exception e) {
			log.info("guiShutdown:local shutdown");
		}
		blocker=false;
	}
	
	

	/******
	 * 
	 * GUI methods and callbacks from GUI for user actions.
	 * You probably do not need to modify anything below here.
	 * 
	 ******/
	
	/**
	 * Redraw the screen with the selected board
	 */
	public void drawSelectedWhiteboard() {
		drawArea.clear();
		if(selectedBoard!=null) {
			selectedBoard.draw(drawArea);
		}
	}
	
	/**
	 * Setup the Swing components and start the Swing thread, given the
	 * peer's specific information, i.e. peer:port string.
	 */
	public void show(String peerport) {
		// create main frame
		JFrame frame = new JFrame("Whiteboard Peer: "+peerport);
		Container content = frame.getContentPane();
		// set layout on content pane
		content.setLayout(new BorderLayout());
		// create draw area
		drawArea = new DrawArea(this);

		// add to content pane
		content.add(drawArea, BorderLayout.CENTER);

		// create controls to apply colors and call clear feature
		JPanel controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

		/**
		 * Action listener is called by the GUI thread.
		 */
		ActionListener actionListener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == clearBtn) {
					clearedLocally();
				} else if (e.getSource() == blackBtn) {
					drawArea.setColor(Color.black);
				} else if (e.getSource() == redBtn) {
					drawArea.setColor(Color.red);
				} else if (e.getSource() == boardComboBox) {
					if(modifyingComboBox) return;
					if(boardComboBox.getSelectedIndex()==-1) return;
					String selectedBoardName=(String) boardComboBox.getSelectedItem();
					if(whiteboards.get(selectedBoardName)==null) {
						System.out.println("get init board");
						getBoardFromPeer(selectedBoardName);
						//log.severe("selected a board that does not exist: "+selectedBoardName);
						//return;
					}
					selectedBoard = whiteboards.get(selectedBoardName);
					// remote boards can't have their shared status modified
					if(selectedBoard.isRemote()) {
						sharedCheckbox.setEnabled(false);
						sharedCheckbox.setVisible(false);
					} else {
						modifyingCheckBox=true;
						sharedCheckbox.setSelected(selectedBoard.isShared());
						modifyingCheckBox=false;
						sharedCheckbox.setEnabled(true);
						sharedCheckbox.setVisible(true);
					}
					selectedABoard();
				} else if (e.getSource() == createBoardBtn) {
					createBoard();
				} else if (e.getSource() == undoBtn) {
					if(selectedBoard==null) {
						log.severe("there is no selected board to undo");
						return;
					}
					undoLocally();
				} else if (e.getSource() == deleteBoardBtn) {
					if(selectedBoard==null) {
						log.severe("there is no selected board to delete");
						return;
					}
					deleteBoard(selectedBoard.getName());
				}
			}
		};
		
		clearBtn = new JButton("Clear Board");
		clearBtn.addActionListener(actionListener);
		clearBtn.setToolTipText("Clear the current board - clears remote copies as well");
		clearBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		blackBtn = new JButton("Black");
		blackBtn.addActionListener(actionListener);
		blackBtn.setToolTipText("Draw with black pen");
		blackBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		redBtn = new JButton("Red");
		redBtn.addActionListener(actionListener);
		redBtn.setToolTipText("Draw with red pen");
		redBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		deleteBoardBtn = new JButton("Delete Board");
		deleteBoardBtn.addActionListener(actionListener);
		deleteBoardBtn.setToolTipText("Delete the current board - only deletes the board locally");
		deleteBoardBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		createBoardBtn = new JButton("New Board");
		createBoardBtn.addActionListener(actionListener);
		createBoardBtn.setToolTipText("Create a new board - creates it locally and not shared by default");
		createBoardBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		undoBtn = new JButton("Undo");
		undoBtn.addActionListener(actionListener);
		undoBtn.setToolTipText("Remove the last path drawn on the board - triggers an undo on remote copies as well");
		undoBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		sharedCheckbox = new JCheckBox("Shared");
		sharedCheckbox.addItemListener(new ItemListener() {    
	         public void itemStateChanged(ItemEvent e) { 
	            if(!modifyingCheckBox) setShare(e.getStateChange()==1);
	         }    
	      }); 
		sharedCheckbox.setToolTipText("Toggle whether the board is shared or not - tells the whiteboard server");
		sharedCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT);
		

		// create a drop list for boards to select from
		JPanel controlsNorth = new JPanel();
		boardComboBox = new JComboBox<String>();
		boardComboBox.addActionListener(actionListener);
		
		
		// add to panel
		controlsNorth.add(boardComboBox);
		controls.add(sharedCheckbox);
		controls.add(createBoardBtn);
		controls.add(deleteBoardBtn);
		controls.add(blackBtn);
		controls.add(redBtn);
		controls.add(undoBtn);
		controls.add(clearBtn);

		// add to content pane
		content.add(controls, BorderLayout.WEST);
		content.add(controlsNorth,BorderLayout.NORTH);

		frame.setSize(600, 600);
		
		// create an initial board
		createBoard();
		
		// closing the application
		frame.addWindowListener(new WindowAdapter() {
		    @Override
		    public void windowClosing(WindowEvent windowEvent) {
		        if (JOptionPane.showConfirmDialog(frame, 
		            "Are you sure you want to close this window?", "Close Window?", 
		            JOptionPane.YES_NO_OPTION,
		            JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
		        {
		        	guiShutdown();
		        	frame.dispose();
		        }
		    }
		});
		
		// show the swing paint result
		frame.setVisible(true);
		
	}
	
	/**
	 * Update the GUI's list of boards. Note that this method needs to update data
	 * that the GUI is using, which should only be done on the GUI's thread, which
	 * is why invoke later is used.
	 * 
	 * @param select, board to select when list is modified or null for default
	 *                selection
	 */
	private void updateComboBox(String select) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				modifyingComboBox=true;
				boardComboBox.removeAllItems();
				int anIndex=-1;
				synchronized(whiteboards) {
					ArrayList<String> boards = new ArrayList<String>(whiteboards.keySet());
					Collections.sort(boards);
					for(int i=0;i<boards.size();i++) {
						String boardname=boards.get(i);
						boardComboBox.addItem(boardname);
						if(select!=null && select.equals(boardname)) {
							anIndex=i;
						} else if(anIndex==-1 && selectedBoard!=null && 
								selectedBoard.getName().equals(boardname)) {
							anIndex=i;
						} 
					}
				}
				modifyingComboBox=false;
				if(anIndex!=-1) {
					boardComboBox.setSelectedIndex(anIndex);
				} else {
					if(whiteboards.size()>0) {
						boardComboBox.setSelectedIndex(0);
					} else {
						drawArea.clear();
						createBoard();
					}
				}
				
			}
		});
	}
	
}
