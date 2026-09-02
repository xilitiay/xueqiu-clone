package com.xueqiu.clone.config;

import com.xueqiu.clone.model.*;
import com.xueqiu.clone.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 演示数据初始化（Mock）。
 * 注意：以下帖子内容均为「示意性」市场评论，用于还原产品形态，
 * 并非真实行情或任何真实作者观点，不构成任何投资建议。
 * 演示账号统一密码为 123456（前端自动以 demo 账号登录）。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final IndexDefRepository indexDefRepository;
    private final WatchlistRepository watchlistRepository;
    private final PositionRepository positionRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final FavoriteRepository favoriteRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, StockRepository stockRepository,
                           PostRepository postRepository, CommentRepository commentRepository,
                           IndexDefRepository indexDefRepository, WatchlistRepository watchlistRepository,
                           PositionRepository positionRepository, CommentLikeRepository commentLikeRepository,
                           FavoriteRepository favoriteRepository, NotificationRepository notificationRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.stockRepository = stockRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.indexDefRepository = indexDefRepository;
        this.watchlistRepository = watchlistRepository;
        this.positionRepository = positionRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.favoriteRepository = favoriteRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return; // 已初始化则跳过

        // ---------- 指数配置（可配置落库；前端侧栏据此展示） ----------
        List.of(
            new IndexDef("SH000001", "上证指数", "沪市", 1),
            new IndexDef("SZ399001", "深证成指", "深市", 2),
            new IndexDef("SZ399006", "创业板指", "深市", 3),
            new IndexDef("SH000300", "沪深300", "沪市", 4),
            new IndexDef("HKHSI", "恒生指数", "港股", 5),
            new IndexDef("USSPX", "标普500", "美股", 6)
        ).forEach(indexDefRepository::save);

        // ---------- 用户（演示账号密码统一 123456） ----------
        User u1 = mkUser("user1", "价值发现者", "#E64340", "长期主义，低估是朋友", 12800);
        User u2 = mkUser("user2", "趋势为王", "#1AAD19", "顺势而为，不预测只跟随", 8600);
        User u3 = mkUser("user3", "稳健派老张", "#3B7CFF", "分散配置，控制回撤", 5300);
        User u4 = mkUser("user4", "科技观察", "#9B59B6", "跟踪半导体与AI产业链", 21000);
        User u5 = mkUser("user5", "小白学投资", "#F39C12", "边学边记，欢迎指教", 920);
        User u6 = mkUser("user6", "宏观笔记", "#16A085", "读宏观数据，看资产价格", 15700);
        User demo = mkUser("demo", "我（演示）", "#E64340", "这是演示账号，前端自动登录", 1);

        // ---------- 股票 ----------
        Stock s1 = stockRepository.save(new Stock("SH600519", "贵州茅台", new BigDecimal("1685.00"),
                new BigDecimal("1.85"), "沪市", "白酒", walk(1680, 30, 12)));
        Stock s2 = stockRepository.save(new Stock("SZ300750", "宁德时代", new BigDecimal("196.40"),
                new BigDecimal("-2.30"), "深市", "电池", walk(200, 30, 9)));
        Stock s3 = stockRepository.save(new Stock("SZ002594", "比亚迪", new BigDecimal("245.10"),
                new BigDecimal("3.12"), "深市", "汽车", walk(238, 30, 8)));
        Stock s4 = stockRepository.save(new Stock("SH601318", "中国平安", new BigDecimal("48.65"),
                new BigDecimal("0.62"), "沪市", "保险", walk(48, 30, 4)));
        Stock s5 = stockRepository.save(new Stock("SH600036", "招商银行", new BigDecimal("35.20"),
                new BigDecimal("-0.45"), "沪市", "银行", walk(35, 30, 3)));
        Stock s6 = stockRepository.save(new Stock("HK00700", "腾讯控股", new BigDecimal("372.80"),
                new BigDecimal("2.05"), "港股", "互联网", walk(365, 30, 7)));
        Stock s7 = stockRepository.save(new Stock("AAPL", "苹果", new BigDecimal("228.50"),
                new BigDecimal("0.95"), "美股", "科技", walk(226, 30, 5)));
        Stock s8 = stockRepository.save(new Stock("NVDA", "英伟达", new BigDecimal("132.10"),
                new BigDecimal("4.40"), "美股", "半导体", walk(126, 30, 11)));

        // ---------- 帖子 ----------
        List<Post> posts = new ArrayList<>();
        posts.add(post(u6, "7月社融数据略超预期，流动性边际改善。从历史经验看，宽货币向宽信用的传导仍需时间，权益资产短期更看风险偏好。", hoursAgo(2), 214, 33, List.of(s4, s5)));
        posts.add(post(u1, "茅台这轮回调后，按今年的预期净利测算，动态市盈率又回到相对舒服的区间。还是那句话：好生意，等好价格。", hoursAgo(4), 532, 88, List.of(s1)));
        posts.add(post(u4, "英伟达下一代算力芯片需求依旧旺盛，产业链订单能见度较高。国产替代方向上，先进封装和HBM值得跟踪。", hoursAgo(5), 891, 142, List.of(s8)));
        posts.add(post(u2, "比亚迪海外交付节奏加快，月销结构里出海占比在提升。趋势没坏，但短期涨幅不小，不追高只低吸。", hoursAgo(7), 376, 51, List.of(s3)));
        posts.add(post(u3, "我的组合里银行+保险压舱，进攻仓位给到电池和半导体。今年目标不是跑赢指数多少，而是把回撤控制在15%以内。", hoursAgo(9), 188, 27, List.of(s2, s4, s5)));
        posts.add(post(u5, "今天第一次搞懂了「市盈率」和「市净率」的区别，记一笔：PE看盈利、PB看净资产。有没有前辈推荐入门书？", hoursAgo(11), 96, 64, List.of()));
        posts.add(post(u6, "美债收益率回落对成长股估值友好，科技龙头的分母端压力缓解。但盈利端还是要逐个看季报。", hoursAgo(14), 421, 39, List.of(s7, s8)));
        posts.add(post(u4, "半导体设备国产化率这几年明显提升，但最尖端的那一层仍然有差距。投资上分清楚「国产替代」和「全球周期」两股力量。", hoursAgo(18), 654, 77, List.of(s8, s2)));
        posts.add(post(u1, "平安的寿险改革走了几年，现在新业务价值增速回正，估值又低，安全边际是有的。耐心持有。", hoursAgo(22), 287, 31, List.of(s4)));
        posts.add(post(u2, "腾讯这波回购很稳，每天雷打不动。对股东友好这事儿，长期会反映在估值里。", hoursAgo(26), 503, 45, List.of(s6)));
        posts.add(post(u3, "招商银行的中收占比在股份行里靠前，零售底盘扎实。作为组合里的「现金等价物」仓位很合适。", hoursAgo(30), 162, 19, List.of(s5)));
        posts.add(post(u5, "跟风买了点宁德，结果赶上回调有点慌。前辈们遇到浮亏一般怎么处理？是补仓还是等企稳？", hoursAgo(34), 134, 58, List.of(s2)));
        // 演示账号自己的帖子（供「通知」示例：别人点赞/评论/回复/@提及 这条帖子）
        posts.add(post(demo, "开个帖子记录自己的组合：银行+保险压舱，成长仓位给到电池和半导体。目标不是跑赢多少，而是把回撤控制住。", hoursAgo(5), 12, 2, List.of(s4, s5)));

        postRepository.saveAll(posts);

        // ---------- 评论 ----------
        List<Comment> comments = new ArrayList<>();
        comments.add(comment(posts.get(1), u5, "受教了，原来好价格比好公司更难等。", hoursAgo(3)));
        comments.add(comment(posts.get(1), u3, "同意，茅台的护城河确实稀缺。", hoursAgo(2)));
        comments.add(comment(posts.get(5), u1, "推荐《聪明的投资者》，先建立框架再下场。", hoursAgo(10)));
        comments.add(comment(posts.get(11), u3, "先看仓位重不重，重就别补，轻仓可以分批。", hoursAgo(33)));
        comments.add(comment(posts.get(11), u6, "浮亏先问自己：买入逻辑变没变？没变就拿着。", hoursAgo(32)));

        // ---------- 嵌套回复示例（演示账号帖子下：顶层评论 + 回复） ----------
        Post demoPost = posts.get(12);
        Comment top = new Comment(demoPost, u1, "组合思路挺稳，压舱+进攻的配置很清晰。", hoursAgo(4));
        top = commentRepository.save(top);
        Comment reply = new Comment(demoPost, u3, "同意，我也是这个思路，回撤先于收益。", hoursAgo(3));
        reply.setParentId(top.getId());   // 回复归入同一顶层（单层嵌套）
        reply = commentRepository.save(reply);
        commentRepository.saveAll(comments);

        // ---------- 评论点赞示例 ----------
        commentLikeRepository.save(new CommentLike(reply.getId(), u1.getId()));
        reply.setLikeCount(1);
        commentLikeRepository.save(new CommentLike(top.getId(), u2.getId()));
        top.setLikeCount(1);
        commentRepository.saveAll(List.of(top, reply));

        // ---------- 收藏示例 ----------
        favoriteRepository.save(new Favorite(posts.get(1).getId(), demo.getId()));
        favoriteRepository.save(new Favorite(posts.get(8).getId(), demo.getId()));

        // ---------- 通知示例（发给演示账号，覆盖各类互动） ----------
        notificationRepository.save(new Notification(demo.getId(), "LIKE_POST", u2.getId(), u2.getName(),
                "POST", demoPost.getId(), "赞了你的帖子"));
        notificationRepository.save(new Notification(demo.getId(), "COMMENT", u1.getId(), u1.getName(),
                "POST", demoPost.getId(), "评论了你的帖子"));
        notificationRepository.save(new Notification(demo.getId(), "REPLY", u3.getId(), u3.getName(),
                "POST", demoPost.getId(), "回复了你的评论"));
        notificationRepository.save(new Notification(demo.getId(), "FOLLOW", u4.getId(), u4.getName(),
                "USER", u4.getId(), "关注了你"));
        notificationRepository.save(new Notification(demo.getId(), "MENTION", u6.getId(), u6.getName(),
                "POST", demoPost.getId(), "在内容中提到了你"));

        // ---------- 演示账号的自选与持仓（前端行情中心 / 持仓 Tab 默认有内容） ----------
        watchlistRepository.save(new Watchlist(demo.getId(), "SH600519", 0)); // 贵州茅台
        watchlistRepository.save(new Watchlist(demo.getId(), "SZ300750", 1)); // 宁德时代
        watchlistRepository.save(new Watchlist(demo.getId(), "SH601318", 2)); // 中国平安
        watchlistRepository.save(new Watchlist(demo.getId(), "HK00700", 3));  // 腾讯控股

        positionRepository.save(new Position(demo.getId(), "SH600519",
                new BigDecimal("100"), new BigDecimal("1500.00"))); // 茅台 100 股 @1500
        positionRepository.save(new Position(demo.getId(), "SZ300750",
                new BigDecimal("500"), new BigDecimal("210.00")));  // 宁德 500 股 @210
    }

    // ---------- 工具方法 ----------
    private User mkUser(String username, String name, String avatarColor, String bio, int followers) {
        User u = new User(name, avatarColor, bio, followers);
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode("123456"));
        return userRepository.save(u);
    }

    private Post post(User author, String content, LocalDateTime createdAt,
                      int likeCount, int commentCount, List<Stock> stocks) {
        return new Post(author, content, createdAt, likeCount, commentCount, stocks);
    }

    private Comment comment(Post post, User author, String content, LocalDateTime createdAt) {
        return new Comment(post, author, content, createdAt);
    }

    private LocalDateTime hoursAgo(int h) {
        return LocalDateTime.now().minusHours(h);
    }

    /** 生成一段确定性的「随机游走」收盘价序列，用于绘制迷你 K 线 */
    private String walk(double base, int n, long seed) {
        Random r = new Random(seed);
        StringBuilder sb = new StringBuilder("[");
        double v = base;
        for (int i = 0; i < n; i++) {
            v += (r.nextDouble() - 0.48) * base * 0.02;
            sb.append(String.format(Locale.US, "%.2f", v));
            if (i < n - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
