import firebase_admin
from firebase_admin import credentials, messaging
from typing import List, Optional
import logging
import os

logger = logging.getLogger(__name__)

class FirebaseService:
    """
    Firebase Cloud Messaging service for sending push notifications
    """
    _initialized = False
    
    @classmethod
    def initialize(cls):
        """Initialize Firebase Admin SDK"""
        if not cls._initialized:
            try:
                # Support both local file and environment variable
                cred_path = os.getenv('FIREBASE_CREDENTIALS_PATH', 'firebase-credentials.json')
                
                if not os.path.exists(cred_path):
                    logger.warning(f"Firebase credentials file not found at {cred_path}")
                    logger.warning("Push notifications will be disabled")
                    return
                
                cred = credentials.Certificate(cred_path)
                firebase_admin.initialize_app(cred)
                cls._initialized = True
                logger.info("✅ Firebase Admin SDK initialized successfully")
            except Exception as e:
                logger.error(f"❌ Failed to initialize Firebase: {e}")
                logger.warning("Push notifications will be disabled")
    
    @classmethod
    def is_initialized(cls) -> bool:
        """Check if Firebase is initialized"""
        return cls._initialized
    
    @classmethod
    def send_notification(
        cls,
        token: str,
        title: str,
        body: str,
        data: Optional[dict] = None,
        notification_type: str = "general"
    ) -> bool:
        """
        Send a push notification to a single device
        
        Args:
            token: FCM device token
            title: Notification title
            body: Notification body
            data: Optional custom data payload
            notification_type: Type of notification (for routing in app)
            
        Returns:
            bool: True if sent successfully
        """
        if not cls._initialized:
            cls.initialize()
        
        if not cls._initialized:
            logger.debug("Firebase not initialized, skipping notification")
            return False
        
        if not token:
            logger.warning("No FCM token provided, skipping notification")
            return False
        
        try:
            # Prepare data payload
            notification_data = data or {}
            notification_data['type'] = notification_type
            notification_data['click_action'] = 'FLUTTER_NOTIFICATION_CLICK'
            
            message = messaging.Message(
                notification=messaging.Notification(
                    title=title,
                    body=body,
                ),
                data=notification_data,
                token=token,
                android=messaging.AndroidConfig(
                    priority='high',
                    notification=messaging.AndroidNotification(
                        channel_id='high_importance_channel',
                        sound='default',
                        color='#FF6F00',  # Orange color for Buyv
                        icon='ic_notification',
                    ),
                ),
                apns=messaging.APNSConfig(
                    headers={
                        'apns-priority': '10',
                    },
                    payload=messaging.APNSPayload(
                        aps=messaging.Aps(
                            sound='default',
                            badge=1,
                            content_available=True,
                        ),
                    ),
                ),
            )
            
            response = messaging.send(message)
            logger.info(f"✅ Successfully sent notification: {response}")
            return True
            
        except messaging.UnregisteredError:
            logger.warning(f"Token is invalid or expired: {token[:20]}...")
            return False
        except Exception as e:
            logger.error(f"❌ Failed to send notification: {e}")
            return False
    
    @classmethod
    def send_multicast(
        cls,
        tokens: List[str],
        title: str,
        body: str,
        data: Optional[dict] = None,
        notification_type: str = "general"
    ) -> dict:
        """
        Send notification to multiple devices
        
        Args:
            tokens: List of FCM device tokens
            title: Notification title
            body: Notification body
            data: Optional custom data payload
            notification_type: Type of notification
            
        Returns:
            dict: {'success_count': int, 'failure_count': int, 'invalid_tokens': list}
        """
        if not cls._initialized:
            cls.initialize()
        
        if not cls._initialized:
            logger.debug("Firebase not initialized, skipping multicast")
            return {
                'success_count': 0,
                'failure_count': len(tokens),
                'invalid_tokens': [],
            }
        
        if not tokens:
            logger.warning("No tokens provided for multicast")
            return {
                'success_count': 0,
                'failure_count': 0,
                'invalid_tokens': [],
            }
        
        try:
            # Prepare data payload
            notification_data = data or {}
            notification_data['type'] = notification_type
            notification_data['click_action'] = 'FLUTTER_NOTIFICATION_CLICK'
            
            message = messaging.MulticastMessage(
                notification=messaging.Notification(
                    title=title,
                    body=body,
                ),
                data=notification_data,
                tokens=tokens,
                android=messaging.AndroidConfig(
                    priority='high',
                    notification=messaging.AndroidNotification(
                        channel_id='high_importance_channel',
                        sound='default',
                        color='#FF6F00',
                        icon='ic_notification',
                    ),
                ),
                apns=messaging.APNSConfig(
                    headers={
                        'apns-priority': '10',
                    },
                    payload=messaging.APNSPayload(
                        aps=messaging.Aps(
                            sound='default',
                            badge=1,
                            content_available=True,
                        ),
                    ),
                ),
            )
            
            response = messaging.send_multicast(message)
            
            # Collect invalid tokens
            invalid_tokens = []
            if response.failure_count > 0:
                for idx, resp in enumerate(response.responses):
                    if not resp.success:
                        if isinstance(resp.exception, messaging.UnregisteredError):
                            invalid_tokens.append(tokens[idx])
            
            logger.info(
                f"✅ Multicast sent: {response.success_count}/{len(tokens)} successful, "
                f"{response.failure_count} failed, {len(invalid_tokens)} invalid tokens"
            )
            
            return {
                'success_count': response.success_count,
                'failure_count': response.failure_count,
                'invalid_tokens': invalid_tokens,
            }
            
        except Exception as e:
            logger.error(f"❌ Failed to send multicast notification: {e}")
            return {
                'success_count': 0,
                'failure_count': len(tokens),
                'invalid_tokens': [],
            }
    
    @classmethod
    def send_to_topic(
        cls,
        topic: str,
        title: str,
        body: str,
        data: Optional[dict] = None,
        notification_type: str = "general"
    ) -> bool:
        """
        Send notification to a topic (all subscribed devices)
        
        Args:
            topic: Topic name (e.g., 'all_users', 'promotions')
            title: Notification title
            body: Notification body
            data: Optional custom data payload
            notification_type: Type of notification
            
        Returns:
            bool: True if sent successfully
        """
        if not cls._initialized:
            cls.initialize()
        
        if not cls._initialized:
            logger.debug("Firebase not initialized, skipping topic notification")
            return False
        
        try:
            notification_data = data or {}
            notification_data['type'] = notification_type
            notification_data['click_action'] = 'FLUTTER_NOTIFICATION_CLICK'
            
            message = messaging.Message(
                notification=messaging.Notification(
                    title=title,
                    body=body,
                ),
                data=notification_data,
                topic=topic,
                android=messaging.AndroidConfig(
                    priority='high',
                    notification=messaging.AndroidNotification(
                        channel_id='high_importance_channel',
                        sound='default',
                        color='#FF6F00',
                        icon='ic_notification',
                    ),
                ),
                apns=messaging.APNSConfig(
                    headers={
                        'apns-priority': '10',
                    },
                    payload=messaging.APNSPayload(
                        aps=messaging.Aps(
                            sound='default',
                            badge=1,
                            content_available=True,
                        ),
                    ),
                ),
            )
            
            response = messaging.send(message)
            logger.info(f"✅ Successfully sent topic notification to '{topic}': {response}")
            return True
            
        except Exception as e:
            logger.error(f"❌ Failed to send topic notification: {e}")
            return False


# Notification type constants
class NotificationType:
    """Notification types for routing in the app"""
    FOLLOW = "follow"
    LIKE = "like"
    COMMENT = "comment"
    ORDER = "order"
    COMMISSION = "commission"
    MESSAGE = "message"
    GENERAL = "general"
    PROMOTION = "promotion"
